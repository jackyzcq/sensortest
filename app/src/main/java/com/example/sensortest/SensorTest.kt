package com.example.sensortest;

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener;
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SensorTest : AppCompatActivity(), SensorEventListener {

    private lateinit var sm: SensorManager

    private var accel: Sensor? = null
    private var gyro: Sensor? = null
    private var light: Sensor? = null
    private var color: Sensor? = null
    private var proximity: Sensor? = null

    private lateinit var tvAccelValue: TextView
    private lateinit var tvGyroValue: TextView
    private lateinit var tvLightValue: TextView
    private lateinit var tvColorValue: TextView
    private lateinit var tvProxValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_1)

        sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyro  = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        light = sm.getDefaultSensor(Sensor.TYPE_LIGHT)
        color = sm.getDefaultSensor(57)
        proximity = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        tvAccelValue = findViewById(R.id.tvAccelValue)
        tvGyroValue  = findViewById(R.id.tvGyroValue)
        tvLightValue = findViewById(R.id.tvLightValue)
        tvColorValue = findViewById(R.id.tvColorValue)
        tvProxValue = findViewById(R.id.tvProxValue)
    }

    override fun onResume() {
        super.onResume()
        accel?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        gyro?.let  { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        light?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        color?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        proximity?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sm.unregisterListener(this)
    }

    override fun onSensorChanged(e: SensorEvent) {
        when (e.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                tvAccelValue.text =
                    String.format(
                        "X: %.3f m/s2\nY: %.3f m/s2\nZ: %.3f m/s2",
                        e.values[0], e.values[1], e.values[2]
                    )
            }

            Sensor.TYPE_GYROSCOPE -> {
                tvGyroValue.text =
                    String.format(
                        "X: %.3f rad/s\nY: %.3f rad/s\nZ: %.3f rad/s",
                        e.values[0], e.values[1], e.values[2]
                    )
            }

            Sensor.TYPE_LIGHT -> {
                tvLightValue.text =
                    String.format("Lux: %.1f lx", e.values[0])
            }
            Sensor.TYPE_PROXIMITY -> {
                tvProxValue.text =
                    String.format("Proximity: %.1f ", e.values[0])
            }
            57 -> {
                tvColorValue.text =
                    String.format("CCT: %.1f cct", e.values[0])
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
