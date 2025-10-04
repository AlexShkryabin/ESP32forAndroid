package com.example.espexample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private val esp32Url = "http://192.168.1.50" // IP ESP32 (из скетча)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnOn = findViewById<Button>(R.id.btnOn)
        val btnOff = findViewById<Button>(R.id.btnOff)

        btnOn.setOnClickListener {
            sendRequest("/led?state=on")
        }

        btnOff.setOnClickListener {
            sendRequest("/led?state=off")
        }
    }

    private fun sendRequest(path: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$esp32Url$path")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
