package com.example.espexample

import android.annotation.SuppressLint
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import java.io.OutputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private val deviceName = "ESP32_Pong_BT" // имя ESP32
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP UUID

    private var outputStream: OutputStream? = null
    private var btSocket: BluetoothSocket? = null
    private var job: Job? = null
    private var sliderValue = 128

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val seekBar = findViewById<SeekBar>(R.id.seekBar)
        seekBar.max = 255
        seekBar.progress = sliderValue

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                sliderValue = progress
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Запускаем подключение к ESP32
        connectToEsp32()
    }

    @SuppressLint("MissingPermission")
    private fun connectToEsp32() {
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth не поддерживается", Toast.LENGTH_LONG).show()
            return
        }

        if (!btAdapter.isEnabled) {
            Toast.makeText(this, "Включите Bluetooth", Toast.LENGTH_LONG).show()
            return
        }

        // Проверяем разрешения для Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                    ),
                    1
                )
                return
            }
        }

        // Проверяем наличие спаренного устройства
        val device: BluetoothDevice? = btAdapter.bondedDevices.firstOrNull { it.name == deviceName }
        if (device == null) {
            Toast.makeText(this, "ESP32 не найден. Сначала спарьте устройство.", Toast.LENGTH_LONG).show()
            return
        }

        try {
            btSocket = device.createRfcommSocketToServiceRecord(uuid)
            btSocket?.connect()
            outputStream = btSocket?.outputStream
            Toast.makeText(this, "Подключено к ESP32!", Toast.LENGTH_SHORT).show()

            // Запускаем периодическую отправку позиции слайдера
            job = CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    try {
                        outputStream?.write(byteArrayOf(sliderValue.toByte()))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    delay(50) // отправка каждые 50 мс
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка подключения к ESP32", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        try {
            outputStream?.close()
            btSocket?.close()
        } catch (_: Exception) {}
    }
}
