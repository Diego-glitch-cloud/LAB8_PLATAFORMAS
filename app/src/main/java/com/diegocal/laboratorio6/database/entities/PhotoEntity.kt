package com.diegocal.laboratorio6.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.diegocal.laboratorio6.Photo
import com.diegocal.laboratorio6.PhotoSrc

/**
 * Entidad que representa una foto en la base de datos local.
 *
 * Índices:
 * - (queryKey, pageIndex): Para búsquedas eficientes por query y página
 * - isFavorite: Para filtrar favoritos rápidamente
 */
@Entity(
    tableName = "photos",
    indices = [
        Index(value = ["queryKey", "pageIndex"]),
        Index(value = ["isFavorite"])
    ]
)
data class PhotoEntity(
    @PrimaryKey
    val id: Int,

    // Dimensiones
    val width: Int,
    val height: Int,

    // Información del autor
    val photographer: String,
    val url: String,

    // URLs de imágenes (almacenadas como strings separados)
    val originalUrl: String,
    val largeUrl: String,
    val mediumUrl: String,
    val smallUrl: String,

    // Metadatos de caché
    val queryKey: String,        // Query normalizada (lowercase, trimmed)
    val pageIndex: Int,           // Página de la que proviene
    val isFavorite: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

// Extensiones para conversión entre Photo (API) y PhotoEntity (DB)

fun Photo.toEntity(queryKey: String, pageIndex: Int, isFavorite: Boolean = false): PhotoEntity {
    return PhotoEntity(
        id = this.id,
        width = this.width,
        height = this.height,
        photographer = this.photographer,
        url = this.url,
        originalUrl = this.imageUrl.original,
        largeUrl = this.imageUrl.large,
        mediumUrl = this.imageUrl.medium,
        smallUrl = this.imageUrl.small,
        queryKey = queryKey,
        pageIndex = pageIndex,
        isFavorite = isFavorite,
        updatedAt = System.currentTimeMillis()
    )
}

fun PhotoEntity.toPhoto(): Photo {
    return Photo(
        id = this.id,
        width = this.width,
        height = this.height,
        photographer = this.photographer,
        url = this.url,
        imageUrl = PhotoSrc(
            original = this.originalUrl,
            large = this.largeUrl,
            medium = this.mediumUrl,
            small = this.smallUrl
        )
    )
}

// Data class para representar una foto con su estado de favorito
data class PhotoWithFavorite(
    val photo: Photo,
    val isFavorite: Boolean
)