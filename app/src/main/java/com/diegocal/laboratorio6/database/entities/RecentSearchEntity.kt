package com.diegocal.laboratorio6.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa una búsqueda reciente.
 *
 * - query es PrimaryKey para evitar duplicados
 * - lastUsedAt se actualiza cada vez que se usa la búsqueda
 * - Ordenamos por lastUsedAt DESC para mostrar las más recientes primero
 */
@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey
    val query: String,              // Query normalizada (lowercase, trimmed)
    val lastUsedAt: Long = System.currentTimeMillis()
)

/**
 * Función de utilidad para normalizar queries.
 * Esto asegura que "Nature", "nature" y " Nature " se traten igual.
 */
fun String.normalizeQuery(): String {
    return this.trim().lowercase()
}