package com.example.aquasample.data.remote.backend

import com.example.aquasample.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Cliente del backend propio, separado del de Open-Meteo. La URL viene de local.properties. */
object ClienteBackend {

    val api: AquaSampleApi by lazy {
        val http = OkHttpClient.Builder()
            // Tiempos cortos: si el backend está apagado, se sabe pronto.
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_URL)
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AquaSampleApi::class.java)
    }
}
