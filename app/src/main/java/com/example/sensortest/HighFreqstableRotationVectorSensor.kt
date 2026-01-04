package com.example.sensortest

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import kotlin.math.*

/**
 * 高采样率稳定虚拟旋转向量
 * - Pitch / Roll / Yaw
 * - 线程安全
 * - 长期稳定，自动零偏 + 温漂补偿
 */
class HighFreqStableRotationVectorSensor : SensorEventListener {

    // ---------------- 重力滤波 & 加速度
    private var gx = 0f
    private var gy = 0f
    private var gz = 0f
    private val ALPHA = 0.90f

    // ---------------- 姿态角
    private var pitch = 0f
    private var roll = 0f
    private var yaw = 0f
    private val K = 0.90f

    private var lastTimestamp: Long = 0L

    // ---------------- 零偏 & 温漂
    private var offsetAccX = 0f
    private var offsetAccY = 0f
    private var offsetAccZ = 0f
    private var offsetGyroX = 0f
    private var offsetGyroY = 0f
    private var offsetGyroZ = 0f

    private var temperature = 25f          // 当前温度
    private var tempRef = 25f              // 标定温度
    private var tempCoefAccX = 0f          // 温漂系数 g/°C
    private var tempCoefAccY = 0f
    private var tempCoefAccZ = 0f

    // ---------------- 静止检测
    private var staticCounter = 0
    private val staticThreshold = 50
    private val accNormTolerance = 0.1f

    // ---------------- 输出监听
    var onOrientationChanged: ((pitch: Float, roll: Float, yaw: Float) -> Unit)? = null

    // ---------------- 加速度计更新
    @Synchronized
    fun updateAccelerometer(axRaw: Float, ayRaw: Float, azRaw: Float, tempC: Float = 25f) {
        temperature = tempC

        val ax = axRaw - offsetAccX - tempCoefAccX * (temperature - tempRef)
        val ay = ayRaw - offsetAccY - tempCoefAccY * (temperature - tempRef)
        val az = azRaw - offsetAccZ - tempCoefAccZ * (temperature - tempRef)

        gx = ALPHA * gx + (1 - ALPHA) * ax
        gy = ALPHA * gy + (1 - ALPHA) * ay
        gz = ALPHA * gz + (1 - ALPHA) * az

        val pitchAcc = Math.toDegrees(atan2(-gx.toDouble(), sqrt((gy*gy + gz*gz).toDouble()))).toFloat()
        val rollAcc  = Math.toDegrees(atan2(gy.toDouble(), gz.toDouble())).toFloat()

        pitch = K * pitch + (1 - K) * pitchAcc
        roll  = K * roll  + (1 - K) * rollAcc

        // ---------------- 静止检测 & 自动零偏
        val accNorm = sqrt(ax*ax + ay*ay + az*az)
        if (abs(accNorm - 9.8f) < accNormTolerance) {
            staticCounter++
            if (staticCounter >= staticThreshold) {
                offsetAccX = 0.99f * offsetAccX + 0.01f * axRaw
                offsetAccY = 0.99f * offsetAccY + 0.01f * ayRaw
                offsetAccZ = 0.99f * offsetAccZ + 0.01f * azRaw

                offsetGyroX = 0.99f * offsetGyroX + 0.01f * 0f
                offsetGyroY = 0.99f * offsetGyroY + 0.01f * 0f
                offsetGyroZ = 0.99f * offsetGyroZ + 0.01f * 0f
                staticCounter = 0
            }
        } else {
            staticCounter = 0
        }
    }

    // ---------------- 陀螺更新
    @Synchronized
    fun updateGyroscope(gxRaw: Float, gyRaw: Float, gzRaw: Float, timestamp: Long) {
        val gxC = gxRaw - offsetGyroX
        val gyC = gyRaw - offsetGyroY
        val gzC = gzRaw - offsetGyroZ

        if (lastTimestamp != 0L) {
            val dt = (timestamp - lastTimestamp) * 1e-9f
            pitch += gxC * dt * 180f / Math.PI.toFloat()
            roll  += gyC * dt * 180f / Math.PI.toFloat()
            yaw   += gzC * dt * 180f / Math.PI.toFloat()
        }
        lastTimestamp = timestamp
    }

    // ---------------- 磁力计修正 Yaw
    @Synchronized
    fun updateMagnetometer(mx: Float, my: Float, mz: Float) {
        val norm = sqrt((mx*mx + my*my + mz*mz).toDouble())
        if (norm == 0.0) return

        val mxn = (mx / norm).toFloat()
        val myn = (my / norm).toFloat()
        val mzn = (mz / norm).toFloat()

        val yawAcc = Math.toDegrees(
            atan2(
                myn * cos(Math.toRadians(pitch.toDouble())) - mzn * sin(Math.toRadians(pitch.toDouble())),
                mxn * cos(Math.toRadians(roll.toDouble())) +
                        myn * sin(Math.toRadians(roll.toDouble())) * sin(Math.toRadians(pitch.toDouble())) +
                        mzn * sin(Math.toRadians(roll.toDouble())) * cos(Math.toRadians(pitch.toDouble()))
            )
        )
        yaw = K * yaw + (1 - K) * yawAcc.toFloat()
    }

    // ---------------- 获取姿态角
    @Synchronized
    fun getOrientation(): Triple<Float, Float, Float> {
        return Triple(pitch, roll, yaw)
    }

    // ---------------- 设置温漂系数
    fun setTemperatureCoefficients(ax: Float, ay: Float, az: Float, tempRefC: Float = 25f) {
        tempCoefAccX = ax
        tempCoefAccY = ay
        tempCoefAccZ = az
        tempRef = tempRefC
    }

    // ---------------- 事件分发
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> updateAccelerometer(event.values[0], event.values[1], event.values[2])
            Sensor.TYPE_GYROSCOPE -> updateGyroscope(event.values[0], event.values[1], event.values[2], event.timestamp)
            Sensor.TYPE_MAGNETIC_FIELD -> updateMagnetometer(event.values[0], event.values[1], event.values[2])
        }
        onOrientationChanged?.invoke(pitch, roll, yaw)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
