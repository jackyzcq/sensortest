package com.example.sensortest

import kotlin.math.*

/**
 * 虚拟 Rotation Vector，兼容 Android TYPE_ROTATION_VECTOR
 * - 返回 float[3]: {x*sin(θ/2), y*sin(θ/2), z*sin(θ/2)}
 * - Pitch / Roll / Yaw 自动融合
 */
class VirtualRotationVector {

    public val rotationSensor = HighFreqStableRotationVectorSensor()

    init {
        // 内部旋转向量回调可更新姿态角，但外部主要通过 getRotationVector() 获取
    }

    /**
     * 获取 Android 坐标系旋转向量 float[3]
     * @return float[3] {x*sin(θ/2), y*sin(θ/2), z*sin(θ/2)}
     */
    @Synchronized
    fun getRotationVector(): FloatArray {
        val (pitch, roll, yaw) = rotationSensor.getOrientation()
        // 将 Pitch/Roll/Yaw 转换为四元数
        val cy = cos(Math.toRadians(yaw.toDouble()) * 0.5)
        val sy = sin(Math.toRadians(yaw.toDouble()) * 0.5)
        val cp = cos(Math.toRadians(pitch.toDouble()) * 0.5)
        val sp = sin(Math.toRadians(pitch.toDouble()) * 0.5)
        val cr = cos(Math.toRadians(roll.toDouble()) * 0.5)
        val sr = sin(Math.toRadians(roll.toDouble()) * 0.5)

        // 四元数 w,x,y,z
        val w = cr*cp*cy + sr*sp*sy
        val x = sr*cp*cy - cr*sp*sy
        val y = cr*sp*cy + sr*cp*sy
        val z = cr*cp*sy - sr*sp*cy

        // 转换为 Android TYPE_ROTATION_VECTOR 输出格式：{x*sin(θ/2), y*sin(θ/2), z*sin(θ/2)}
        val theta = 2 * acos(w)
        val s = if (theta < 1e-6) 1.0 else sin(theta/2)/sin(theta/2)
        val out = FloatArray(3)
        out[0] = (x * sin(theta/2)).toFloat()
        out[1] = (y * sin(theta/2)).toFloat()
        out[2] = (z * sin(theta/2)).toFloat()
        return out
    }

    // ---------------- 代理方法，更新传感器数据
    fun updateAccelerometer(ax: Float, ay: Float, az: Float, tempC: Float = 25f) {
        rotationSensor.updateAccelerometer(ax, ay, az, tempC)
    }

    fun updateGyroscope(gx: Float, gy: Float, gz: Float, timestamp: Long) {
        rotationSensor.updateGyroscope(gx, gy, gz, timestamp)
    }

    fun updateMagnetometer(mx: Float, my: Float, mz: Float) {
        rotationSensor.updateMagnetometer(mx, my, mz)
    }

    fun setTemperatureCoefficients(ax: Float, ay: Float, az: Float, tempRefC: Float = 25f) {
        rotationSensor.setTemperatureCoefficients(ax, ay, az, tempRefC)
    }
}
