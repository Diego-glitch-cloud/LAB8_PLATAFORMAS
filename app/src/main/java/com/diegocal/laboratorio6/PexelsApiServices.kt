package com.diegocal.laboratorio6

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url


interface PexelsApiService {
    @GET("v1/search")
    suspend fun searchPhotos(
        @Query("query") query: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): PexelsResponse

    @GET
    suspend fun getNextPage(@Url nextUrl: String): PexelsResponse

    @GET("v1/photos/{id}")
    suspend fun getPhotoById(@Path("id") id: Int): Photo
}