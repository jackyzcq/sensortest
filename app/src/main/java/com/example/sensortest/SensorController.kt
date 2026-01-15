package com.example.sensortest

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLSurfaceView
import android.view.Surface
import android.view.WindowManager

class SensorController(
    private val glView: GLSurfaceView,
    private val renderer: OrientationRenderer
) : SensorEventListener {

    private val virtualVector = VirtualRotationVector()

    private val sensorManager =
        glView.context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val acc = FloatArray(3)
    private var yaw = 0f
    private var lastGyroTs = 0L

    fun start() {
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_UI
        )
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
            SensorManager.SENSOR_DELAY_UI
        )
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
            SensorManager.SENSOR_DELAY_UI
        )
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }
    private fun rad2deg(r: Float) = (r * 180f / Math.PI).toFloat()

    private fun adjustForScreenRotation(pitch: Float, roll: Float, yaw: Float): Triple<Float, Float, Float> {
        val rotation = (glView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            .defaultDisplay.rotation

        return when (rotation) {
            Surface.ROTATION_0 -> Triple(pitch, roll, yaw)
            Surface.ROTATION_90 -> Triple(-roll, pitch, yaw)
            Surface.ROTATION_180 -> Triple(-pitch, -roll, yaw)
            Surface.ROTATION_270 -> Triple(roll, -pitch, yaw)
            else -> Triple(pitch, roll, yaw)
        }
    }

    override fun onSensorChanged(e: SensorEvent) {
        when (e.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(e.values, 0, acc, 0, 3)
                virtualVector.updateAccelerometer(e.values[0], e.values[1], e.values[2])
            }

            Sensor.TYPE_GYROSCOPE -> {
                if (lastGyroTs != 0L) {
                    val dt = (e.timestamp - lastGyroTs) * 1e-9f
                    yaw += e.values[2] * dt
                }
                lastGyroTs = e.timestamp
                virtualVector.updateGyroscope(e.values[0], e.values[1], e.values[2], e.timestamp)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> virtualVector.updateMagnetometer(e.values[0], e.values[1], e.values[2])
        }

        val pitch = rad2deg(
            kotlin.math.atan2(-acc[0], kotlin.math.sqrt(acc[1]*acc[1] + acc[2]*acc[2]))
        )
        val roll = rad2deg(
            kotlin.math.atan2(acc[1], acc[2])
        )

        val rotationVector = virtualVector.getRotationVector()
        val orientation = virtualVector.rotationSensor.getOrientation()

        val (adjPitch, adjRoll, adjYaw) = adjustForScreenRotation(orientation.first, orientation.second, orientation.third)

        // ⭐ 关键：切到 GL 线程，只传数据
        glView.queueEvent {
            //renderer.setRotation(pitch, roll, rad2deg(yaw))
            renderer.setRotation(orientation.first, orientation.second, orientation.third)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
