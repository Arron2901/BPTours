package com.example.bptoursapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class LoadingActivity : AppCompatActivity() {

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_loading)
//
//        fetchWeather()
//        Handler(Looper.getMainLooper()).postDelayed({
//            val intent = Intent(this, MainActivity::class.java) // Navigate to MainActivity
//            startActivity(intent)
//            finish()
//        }, 2000)
//
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        // Fetch weather, then navigate when done
        fetchWeather {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}

fun formatDate(dateString: String): String {
    val inputFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME // Parses "2025-02-07T05:00:00Z"
    val outputFormatter = DateTimeFormatter.ofPattern("dd/MM") // Converts to "07/02"

    val date = ZonedDateTime.parse(dateString, inputFormatter)
    return date.format(outputFormatter)
}

fun parseWeatherData(jsonResponse: String) {
    val jsonObject = JSONObject(jsonResponse)
    val timelines = jsonObject.getJSONObject("data")
        .getJSONArray("timelines")
        .getJSONObject(0)
        .getJSONArray("intervals")

    val datesList = MainActivity.GlobalWeatherVars.dates
    val temperaturesList = MainActivity.GlobalWeatherVars.temperaturesList
    val weatherCodesList = MainActivity.GlobalWeatherVars.weatherCodesList

    for (i in 0 until timelines.length()) {
        val interval = timelines.getJSONObject(i)
        val date = interval.getString("startTime")
        val formattedDate = formatDate(date)
        val values = interval.getJSONObject("values")
        val temperature = values.getDouble("temperature")
        val weatherCodeFullDay = values.getInt("weatherCode")

        // Add values to lists
        datesList.add(formattedDate)
        temperaturesList.add(temperature)
        weatherCodesList.add(weatherCodeFullDay)
    }

}


fun fetchWeather(onComplete: () -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val client = OkHttpClient()

            val mediaType = "application/json".toMediaTypeOrNull()
            val body =
                "{\"location\":\"Blackpool\",\"fields\":[\"temperature\",\"weatherCode\"],\"units\":\"metric\",\"timesteps\":[\"1d\"],\"startTime\":\"now\",\"endTime\":\"nowPlus4d\"}"
                    .toRequestBody(mediaType)

            val request = Request.Builder()
                .url("https://api.tomorrow.io/v4/timelines?apikey=sV8MgHEnJD6DX5CCKMQw4fUv5JVrINoL")
                .post(body)
                .addHeader("accept", "application/json")

                .addHeader("content-type", "application/json")
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                println("Error: ${response.code}")
            } else {
                val data = response.body?.string()
                if (data != null) {
                    parseWeatherData(data)
                }
            }

            withContext(Dispatchers.Main) {
                onComplete() // Notify that fetch is done
            }

        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onComplete() // Notify that fetch is done
            }
        }
    }
}
