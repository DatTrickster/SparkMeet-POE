package com.example.sparkmeet.apis

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

// Request & Response models
data class PersonaRequest(val imageBase64: String, val uid: String)
data class PersonaResponse(val vector: List<Float>?)

// Retrofit interface
interface PersonaApiService {
    @Headers("Content-Type: application/json")
    @POST("renderPersona")
    fun sendPersona(@Body request: PersonaRequest): Call<PersonaResponse>
}

object FaceApi {

    private const val BASE_URL = "http://192.168.31.75:8000/"

    private val apiService: PersonaApiService by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PersonaApiService::class.java)
    }

    fun sendPersonaImage(
        context: Context,
        uid: String,
        imageBase64: String,
        onResult: (vector: List<Float>?) -> Unit
    ) {
        println("[LOG] Sending image to API for UID: $uid")
        println("[LOG] Base64 length: ${imageBase64.length}")

        val request = PersonaRequest(imageBase64 = imageBase64, uid = uid)
        apiService.sendPersona(request).enqueue(object : Callback<PersonaResponse> {
            override fun onResponse(
                call: Call<PersonaResponse>,
                response: Response<PersonaResponse>
            ) {
                println("[LOG] Response code: ${response.code()}")
                if (response.isSuccessful) {
                    val vector = response.body()?.vector
                    println("[LOG] Received vector length: ${vector?.size}")
                    onResult(vector)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Failed to detect face"
                    println("[WARN] API returned error: $errorMsg")
                    showToast(context, "No Face Detected: $errorMsg")
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<PersonaResponse>, t: Throwable) {
                println("[ERROR] Error sending Persona image: ${t.message}")
                showToast(context, "Error: ${t.message ?: "Failed to send Persona image"}")
                onResult(null)
            }
        })
    }

    private fun showToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
