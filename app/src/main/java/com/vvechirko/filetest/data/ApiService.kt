package com.vvechirko.filetest.data

import io.reactivex.Observable
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

object ApiService {

    const val ENDPOINT = "https://jsonplaceholder.typicode.com/"

    fun <S> create(clazz: Class<S>) = Retrofit.Builder()
        .baseUrl(ENDPOINT)
        .client(
            OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build()
        )
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(clazz)
}

interface Api {

    @GET("albums")
    fun getAlbums(@Query("userId") userId: Int): Observable<List<Album>>

    @GET("albums/{albumId}")
    fun getAlbum(@Path("albumId") albumId: Int): Observable<Album>

    @GET("photos")
    fun getPhotos(@Query("albumId") albumId: Int): Observable<List<Photo>>

    @GET("photos/{photoId}")
    fun getPhoto(@Path("photoId") photoId: Int): Observable<Photo>

    @GET()
    fun download(@Url url: String): Observable<ResponseBody>
}

data class Photo(
    val id: Int,
    val albumId: Int,
    val title: String,
    val url: String,
    val thumbnailUrl: String
) {
    val name: String
        get() = "photo-$id.jpg"

    val thumbName: String
        get() = "thumb-$id.jpg"
}

data class Album(
    val id: Int,
    val userId: Int,
    val title: String
)