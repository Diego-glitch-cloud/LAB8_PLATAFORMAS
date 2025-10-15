package com.diegocal.laboratorio6.database.dao

import androidx.room.*
import com.diegocal.laboratorio6.database.entities.RecentSearchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentSearchDao {

    /**
     * Inserta o actualiza una búsqueda reciente.
     * Si la query ya existe, actualiza lastUsedAt.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentSearch(search: RecentSearchEntity)

    /**
     * Obtiene las N búsquedas más recientes como Flow.
     * Se actualiza automáticamente cuando cambia la BD.
     */
    @Query("""
        SELECT * FROM recent_searches 
        ORDER BY lastUsedAt DESC 
        LIMIT :limit
    """)
    fun getRecentSearches(limit: Int = 10): Flow<List<RecentSearchEntity>>

    /**
     * Obtiene las N búsquedas más recientes (suspending function).
     */
    @Query("""
        SELECT * FROM recent_searches 
        ORDER BY lastUsedAt DESC 
        LIMIT :limit
    """)
    suspend fun getRecentSearchesList(limit: Int = 10): List<RecentSearchEntity>

    /**
     * Elimina una búsqueda específica.
     */
    @Query("DELETE FROM recent_searches WHERE query = :query")
    suspend fun deleteRecentSearch(query: String)

    /**
     * Limpia todas las búsquedas recientes.
     */
    @Query("DELETE FROM recent_searches")
    suspend fun clearAllRecentSearches()

    /**
     * Mantiene solo las N búsquedas más recientes.
     * Elimina las más antiguas que excedan el límite.
     */
    @Query("""
        DELETE FROM recent_searches 
        WHERE query NOT IN (
            SELECT query FROM recent_searches 
            ORDER BY lastUsedAt DESC 
            LIMIT :limit
        )
    """)
    suspend fun keepOnlyRecentN(limit: Int = 10)
}