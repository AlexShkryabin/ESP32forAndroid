package com.example.espexample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private val esp32Url = "http://192.168.4.1" // IP точки доступа ESP32
    private var currentAngle = 90
    private val step = 10 // шаг изменения угла

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvAngle = findViewById<TextView>(R.id.tvAngle)
        val btnIncrease = findViewById<Button>(R.id.btnIncrease)
        val btnDecrease = findViewById<Button>(R.id.btnDecrease)

        fun updateAngleDisplay() {
            tvAngle.text = "Угол: $currentAngle°"
        }

        btnIncrease.setOnClickListener {
            currentAngle = (currentAngle + step).coerceAtMost(180)
            updateAngleDisplay()
            sendRequest("/servo?angle=$currentAngle")
        }

        btnDecrease.setOnClickListener {
            currentAngle = (currentAngle - step).coerceAtLeast(0)
            updateAngleDisplay()
            sendRequest("/servo?angle=$currentAngle")
        }

        updateAngleDisplay()

        // Периодически обновляем статус с ESP32
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                try {
                    val status = getRequest("/status")
                    if (status.isNotEmpty()) {
                        currentAngle = status.toIntOrNull() ?: currentAngle
                        runOnUiThread { updateAngleDisplay() }
                    }
                    Thread.sleep(2000)
                } catch (_: Exception) { }
            }
        }
    }

    private fun sendRequest(path: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$esp32Url$path")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 2000
                connection.readTimeout = 2000
                connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getRequest(path: String): String {
        return try {
            val url = URL("$esp32Url$path")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 2000
            connection.readTimeout = 2000
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            response
        } catch (e: Exception) {
            ""
        }
    }
}
