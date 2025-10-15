package com.diegocal.laboratorio6

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Foto individual
@JsonClass(generateAdapter = true)
data class Photo(
    val id: Int,
    val width: Int,
    val height: Int,
    val url: String,
    val photographer: String,
    @Json(name = "src") val imageUrl: PhotoSrc // para traducir a moshi
)

// Resoluciones.
@JsonClass(generateAdapter = true)
data class PhotoSrc(
    val original: String,
    val large: String,
    val medium: String,
    val small: String
)

// Respuesta de la API.
@JsonClass(generateAdapter = true)
data class PexelsResponse(
    val photos: List<Photo>,
    @Json(name = "next_page") val nextPageUrl: String?
)