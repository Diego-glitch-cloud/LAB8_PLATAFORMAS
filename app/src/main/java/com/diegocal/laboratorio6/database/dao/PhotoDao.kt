package com.diegocal.laboratorio6.database.dao

import androidx.room.*
import com.diegocal.laboratorio6.database.entities.PhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {

    // ==================== INSERCIÓN ====================

    /**
     * Inserta fotos, reemplazando si ya existen.
     * Útil para actualizar caché.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity)

    // ==================== CONSULTAS POR QUERY ====================

    /**
     * Obtiene fotos para una query específica, ordenadas por página.
     * Retorna Flow para observar cambios.
     */
    @Query("""
        SELECT * FROM photos 
        WHERE queryKey = :queryKey 
        ORDER BY pageIndex ASC, id ASC
    """)
    fun getPhotosByQueryFlow(queryKey: String): Flow<List<PhotoEntity>>

    /**
     * Obtiene fotos para una query específica (suspending).
     */
    @Query("""
        SELECT * FROM photos 
        WHERE queryKey = :queryKey 
        ORDER BY pageIndex ASC, id ASC
    """)
    suspend fun getPhotosByQuery(queryKey: String): List<PhotoEntity>

    /**
     * Obtiene fotos para una query y página específicas.
     * Útil para verificar si una página ya está en caché.
     */
    @Query("""
        SELECT * FROM photos 
        WHERE queryKey = :queryKey AND pageIndex = :page
        ORDER BY id ASC
    """)
    suspend fun getPhotosByQueryAndPage(queryKey: String, page: Int): List<PhotoEntity>

    /**
     * Obtiene fotos con límite y offset (paginación manual).
     */
    @Query("""
        SELECT * FROM photos 
        WHERE queryKey = :queryKey 
        ORDER BY pageIndex ASC, id ASC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getPhotosByQueryPaginated(
        queryKey: String,
        limit: Int,
        offset: Int
    ): List<PhotoEntity>

    /**
     * Cuenta cuántas fotos hay para una query específica.
     */
    @Query("SELECT COUNT(*) FROM photos WHERE queryKey = :queryKey")
    suspend fun getPhotosCountForQuery(queryKey: String): Int

    // ==================== CONSULTA POR ID ====================

    /**
     * Obtiene una foto específica por su ID.
     */
    @Query("SELECT * FROM photos WHERE id = :photoId LIMIT 1")
    suspend fun getPhotoById(photoId: Int): PhotoEntity?

    /**
     * Flow para observar cambios en una foto específica.
     */
    @Query("SELECT * FROM photos WHERE id = :photoId LIMIT 1")
    fun observePhotoById(photoId: Int): Flow<PhotoEntity?>

    // ==================== FAVORITOS ====================

    /**
     * Actualiza el estado de favorito de una foto.
     */
    @Query("UPDATE photos SET isFavorite = :isFavorite WHERE id = :photoId")
    suspend fun updateFavoriteStatus(photoId: Int, isFavorite: Boolean)

    /**
     * Obtiene todas las fotos favoritas.
     */
    @Query("SELECT * FROM photos WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoritePhotos(): Flow<List<PhotoEntity>>

    /**
     * Obtiene todas las fotos favoritas (suspending).
     */
    @Query("SELECT * FROM photos WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    suspend fun getFavoritePhotosList(): List<PhotoEntity>

    /**
     * Verifica si una foto es favorita.
     */
    @Query("SELECT isFavorite FROM photos WHERE id = :photoId")
    suspend fun isFavorite(photoId: Int): Boolean?

    // ==================== LIMPIEZA DE CACHÉ ====================

    /**
     * Elimina fotos antiguas para una query específica.
     * @param maxAge Edad máxima en milisegundos (ej: 24 horas = 86400000)
     */
    @Query("""
        DELETE FROM photos 
        WHERE queryKey = :queryKey 
        AND isFavorite = 0 
        AND updatedAt < :maxAge
    """)
    suspend fun deleteOldPhotosForQuery(queryKey: String, maxAge: Long)

    /**
     * Elimina todas las fotos de una query (excepto favoritas).
     */
    @Query("DELETE FROM photos WHERE queryKey = :queryKey AND isFavorite = 0")
    suspend fun deletePhotosForQuery(queryKey: String)

    /**
     * Elimina TODAS las fotos (incluso favoritas) de una query.
     * Útil para forzar actualización de caché.
     */
    @Query("DELETE FROM photos WHERE queryKey = :queryKey")
    suspend fun deleteAllPhotosForQuery(queryKey: String)

    /**
     * Limpia todo el caché (mantiene favoritos).
     */
    @Query("DELETE FROM photos WHERE isFavorite = 0")
    suspend fun clearCache()

    /**
     * Elimina TODO (incluso favoritos). Usar con precaución.
     */
    @Query("DELETE FROM photos")
    suspend fun deleteAll()
}