package com.diegocal.laboratorio6.repository

import android.util.Log
import com.diegocal.laboratorio6.Photo
import com.diegocal.laboratorio6.PexelsApiService
import com.diegocal.laboratorio6.database.dao.PhotoDao
import com.diegocal.laboratorio6.database.dao.RecentSearchDao
import com.diegocal.laboratorio6.database.entities.PhotoEntity
import com.diegocal.laboratorio6.database.entities.RecentSearchEntity
import com.diegocal.laboratorio6.database.entities.normalizeQuery
import com.diegocal.laboratorio6.database.entities.toEntity
import com.diegocal.laboratorio6.database.entities.toPhoto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repositorio que maneja la lógica de negocio para fotos.
 *
 * Estrategia: Cache-first con network fallback
 * 1. Intenta cargar desde caché local
 * 2. Si no hay datos o están obsoletos, consulta la red
 * 3. Persiste los resultados de red en caché
 */
class PhotoRepository(
    private val photoDao: PhotoDao,
    private val recentSearchDao: RecentSearchDao,
    private val apiService: PexelsApiService
) {
    companion object {
        private const val TAG = "PhotoRepository"
        private const val CACHE_MAX_AGE_MS = 24 * 60 * 60 * 1000L // 24 horas
    }

    // ==================== BÚSQUEDA DE FOTOS ====================

    /**
     * Busca fotos con estrategia cache-first.
     *
     * @return Resultado con fotos y si hay siguiente página
     */
    suspend fun searchPhotos(
        query: String,
        page: Int,
        perPage: Int = 15,
        forceRefresh: Boolean = false
    ): Result<SearchResult> {
        val normalizedQuery = query.normalizeQuery()

        return try {
            // 1. Registrar búsqueda reciente
            saveRecentSearch(normalizedQuery)

            // 2. Intentar cargar desde caché si no es refresh forzado
            if (!forceRefresh) {
                val cachedPhotos = photoDao.getPhotosByQueryAndPage(normalizedQuery, page)
                if (cachedPhotos.isNotEmpty()) {
                    Log.d(TAG, "Cache HIT: $normalizedQuery, page $page")
                    return Result.success(
                        SearchResult(
                            photos = cachedPhotos.map { it.toPhoto() },
                            hasNextPage = true, // Asumimos que hay más páginas
                            fromCache = true
                        )
                    )
                }
            }

            // 3. Cache MISS o refresh forzado: consultar red
            Log.d(TAG, "Network fetch: $normalizedQuery, page $page")
            val response = apiService.searchPhotos(query, page, perPage)

            // 4. Persistir resultados
            val entities = response.photos.map { photo ->
                // Mantener estado de favorito si ya existía
                val existingPhoto = photoDao.getPhotoById(photo.id)
                photo.toEntity(
                    queryKey = normalizedQuery,
                    pageIndex = page,
                    isFavorite = existingPhoto?.isFavorite ?: false
                )
            }
            photoDao.insertPhotos(entities)

            // 5. Limpiar caché antiguo para esta query
            cleanOldCache(normalizedQuery)

            Result.success(
                SearchResult(
                    photos = response.photos,
                    hasNextPage = response.nextPageUrl != null,
                    fromCache = false
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error en búsqueda: ${e.message}", e)

            // Fallback: intentar caché aunque sea antiguo
            val cachedPhotos = photoDao.getPhotosByQueryAndPage(normalizedQuery, page)
            if (cachedPhotos.isNotEmpty()) {
                Log.d(TAG, "Usando caché antiguo por error de red")
                Result.success(
                    SearchResult(
                        photos = cachedPhotos.map { it.toPhoto() },
                        hasNextPage = false,
                        fromCache = true
                    )
                )
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Obtiene la última query buscada desde caché.
     * Útil para modo offline.
     */
    suspend fun getLastSearchedQuery(): String? {
        return recentSearchDao.getRecentSearchesList(1).firstOrNull()?.query
    }

    /**
     * Obtiene fotos cacheadas para la última búsqueda.
     * Para modo offline básico.
     */
    suspend fun getCachedPhotosForLastSearch(): List<Photo> {
        val lastQuery = getLastSearchedQuery() ?: return emptyList()
        val entities = photoDao.getPhotosByQueryAndPage(lastQuery, 1)
        return entities.map { it.toPhoto() }
    }

    // ==================== DETALLES DE FOTO ====================

    /**
     * Obtiene una foto por ID con estrategia cache-first.
     */
    suspend fun getPhotoById(photoId: Int): Result<Photo> {
        return try {
            // 1. Intentar desde caché
            val cachedPhoto = photoDao.getPhotoById(photoId)
            if (cachedPhoto != null) {
                Log.d(TAG, "Photo cache HIT: $photoId")
                return Result.success(cachedPhoto.toPhoto())
            }

            // 2. Consultar red
            Log.d(TAG, "Network fetch photo: $photoId")
            val photo = apiService.getPhotoById(photoId)

            // 3. Persistir (sin query específica, usar "detail" como queryKey)
            val existingPhoto = photoDao.getPhotoById(photo.id)
            val entity = photo.toEntity(
                queryKey = "detail",
                pageIndex = 0,
                isFavorite = existingPhoto?.isFavorite ?: false
            )
            photoDao.insertPhoto(entity)

            Result.success(photo)

        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo foto $photoId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Observa una foto por ID (Flow para UI reactiva).
     */
    fun observePhotoById(photoId: Int): Flow<Photo?> {
        return photoDao.observePhotoById(photoId).map { it?.toPhoto() }
    }

    // ==================== FAVORITOS ====================

    /**
     * Alterna el estado de favorito de una foto.
     */
    suspend fun toggleFavorite(photoId: Int) {
        val currentStatus = photoDao.isFavorite(photoId) ?: false
        photoDao.updateFavoriteStatus(photoId, !currentStatus)
        Log.d(TAG, "Favorito actualizado: $photoId -> ${!currentStatus}")
    }

    /**
     * Establece el estado de favorito.
     */
    suspend fun setFavorite(photoId: Int, isFavorite: Boolean) {
        photoDao.updateFavoriteStatus(photoId, isFavorite)
    }

    /**
     * Verifica si una foto es favorita.
     */
    suspend fun isFavorite(photoId: Int): Boolean {
        return photoDao.isFavorite(photoId) ?: false
    }

    /**
     * Obtiene todas las fotos favoritas como Flow.
     */
    fun getFavoritePhotos(): Flow<List<Photo>> {
        return photoDao.getFavoritePhotos().map { entities ->
            entities.map { it.toPhoto() }
        }
    }

    // ==================== BÚSQUEDAS RECIENTES ====================

    /**
     * Guarda una búsqueda reciente.
     */
    private suspend fun saveRecentSearch(query: String) {
        val normalizedQuery = query.normalizeQuery()
        if (normalizedQuery.isNotBlank()) {
            recentSearchDao.insertRecentSearch(
                RecentSearchEntity(
                    query = normalizedQuery,
                    lastUsedAt = System.currentTimeMillis()
                )
            )
            // Mantener solo las últimas 10
            recentSearchDao.keepOnlyRecentN(10)
        }
    }

    /**
     * Obtiene búsquedas recientes como Flow.
     */
    fun getRecentSearches(limit: Int = 10): Flow<List<String>> {
        return recentSearchDao.getRecentSearches(limit).map { searches ->
            searches.map { it.query }
        }
    }

    /**
     * Elimina una búsqueda reciente.
     */
    suspend fun deleteRecentSearch(query: String) {
        recentSearchDao.deleteRecentSearch(query.normalizeQuery())
    }

    /**
     * Limpia todas las búsquedas recientes.
     */
    suspend fun clearRecentSearches() {
        recentSearchDao.clearAllRecentSearches()
    }

    // ==================== LIMPIEZA DE CACHÉ ====================

    /**
     * Limpia caché antiguo para una query específica.
     */
    private suspend fun cleanOldCache(queryKey: String) {
        val maxAge = System.currentTimeMillis() - CACHE_MAX_AGE_MS
        photoDao.deleteOldPhotosForQuery(queryKey, maxAge)
    }

    /**
     * Limpia todo el caché (mantiene favoritos).
     */
    suspend fun clearCache() {
        photoDao.clearCache()
        Log.d(TAG, "Caché limpiado (favoritos mantenidos)")
    }

    /**
     * Fuerza actualización de caché para una query.
     */
    suspend fun refreshCache(query: String) {
        val normalizedQuery = query.normalizeQuery()
        photoDao.deletePhotosForQuery(normalizedQuery)
        Log.d(TAG, "Caché invalidado para: $normalizedQuery")
    }
}

/**
 * Data class para resultado de búsqueda.
 */
data class SearchResult(
    val photos: List<Photo>,
    val hasNextPage: Boolean,
    val fromCache: Boolean
)