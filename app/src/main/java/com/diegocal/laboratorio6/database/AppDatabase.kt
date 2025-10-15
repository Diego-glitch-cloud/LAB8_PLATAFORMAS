package com.diegocal.laboratorio6.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.diegocal.laboratorio6.database.dao.PhotoDao
import com.diegocal.laboratorio6.database.dao.RecentSearchDao
import com.diegocal.laboratorio6.database.entities.PhotoEntity
import com.diegocal.laboratorio6.database.entities.RecentSearchEntity

/**
 * Base de datos principal de la aplicación.
 *
 * Versión 1: Schema inicial con photos y recent_searches
 */
@Database(
    entities = [
        PhotoEntity::class,
        RecentSearchEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun photoDao(): PhotoDao
    abstract fun recentSearchDao(): RecentSearchDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Obtiene la instancia singleton de la base de datos.
         * Thread-safe usando double-checked locking.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pexels_gallery_database"
                )
                    // Opcional: Agregar migración en el futuro
                    // .addMigrations(MIGRATION_1_2)

                    // Solo para desarrollo: descomentar para eliminar BD en cambios de schema
                    // .fallbackToDestructiveMigration()

                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Limpia la instancia (útil para testing).
         */
        fun clearInstance() {
            INSTANCE = null
        }
    }
}