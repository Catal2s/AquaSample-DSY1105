package com.example.aquasample.data.remote.marina

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente Retrofit de Open-Meteo. Es independiente del que se use para los
 * microservicios propios: si esta API falla, no afecta al backend.
 */
object ClienteOpenMeteo {

    private const val URL_BASE = "https://marine-api.open-meteo.com/"

    val api: OpenMeteoMarineApi by lazy {
        Retrofit.Builder()
            .baseUrl(URL_BASE)
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .callTimeout(25, TimeUnit.SECONDS)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenMeteoMarineApi::class.java)
    }
}
