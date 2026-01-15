package com.example.sensortest
import android.view.Surface

data class Quaternion(
    var w: Float = 1f,
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f
) {
    fun normalize() {
        val n = kotlin.math.sqrt(w*w + x*x + y*y + z*z)
        if (n < 1e-6f) return
        w /= n; x /= n; y /= n; z /= n
    }

    fun multiply(b: Quaternion): Quaternion {
        return Quaternion(
            w*b.w - x*b.x - y*b.y - z*b.z,
            w*b.x + x*b.w + y*b.z - z*b.y,
            w*b.y - x*b.z + y*b.w + z*b.x,
            w*b.z + x*b.y - y*b.x + z*b.w
        )
    }
    fun integrateGyro(wx: Float, wy: Float, wz: Float, dt: Float) {
        val halfDt = 0.5f * dt
        val dq = Quaternion(1f, wx*halfDt, wy*halfDt, wz*halfDt)
        val qNew = multiply(dq)
        //val qNew = dq.multiply(this)   // ← 关键修正
        w = qNew.w; x = qNew.x; y = qNew.y; z = qNew.z
        normalize()
    }

    fun fromAxisAngle(
        ax: Float,
        ay: Float,
        az: Float,
        angleDeg: Float
    ): Quaternion {

        // 1. 角度 → 弧度
        val rad = Math.toRadians(angleDeg.toDouble()).toFloat()

        // 2. 半角
        val half = rad * 0.5f
        val sinH = kotlin.math.sin(half)
        val cosH = kotlin.math.cos(half)

        // 3. 轴归一化（非常重要）
        val norm = kotlin.math.sqrt(ax*ax + ay*ay + az*az)
        if (norm < 1e-6f) {
            return Quaternion(1f, 0f, 0f, 0f)
        }

        val ux = ax / norm
        val uy = ay / norm
        val uz = az / norm

        // 4. 构造 quaternion
        return Quaternion(
            w = cosH,
            x = ux * sinH,
            y = uy * sinH,
            z = uz * sinH
        )
    }

    fun correctWithAccel(ax: Float, ay: Float, az: Float, alpha: Float = 0.02f) {
        val norm = kotlin.math.sqrt(ax*ax + ay*ay + az*az)
        if (norm < 1e-6f) return

        val gx = ax / norm
        val gy = ay / norm
        val gz = az / norm

        val vx = 2f * (x*z - w*y)
        val vy = 2f * (w*x + y*z)
        val vz = w*w - x*x - y*y + z*z

        val ex = (gy*vz - gz*vy)
        val ey = (gz*vx - gx*vz)
        val ez = (gx*vy - gy*vx)

        integrateGyro(ex * alpha, ey * alpha, ez * alpha, 1f)
    }
    fun toMatrix(m: FloatArray) {
        m[0] = 1 - 2*y*y - 2*z*z
        m[1] = 2*x*y + 2*w*z
        m[2] = 2*x*z - 2*w*y
        m[3] = 0f

        m[4] = 2*x*y - 2*w*z
        m[5] = 1 - 2*x*x - 2*z*z
        m[6] = 2*y*z + 2*w*x
        m[7] = 0f

        m[8]  = 2*x*z + 2*w*y
        m[9]  = 2*y*z - 2*w*x
        m[10] = 1 - 2*x*x - 2*y*y
        m[11] = 0f

        m[12] = 0f
        m[13] = 0f
        m[14] = 0f
        m[15] = 1f
    }
    fun toGLMatrix(m: FloatArray) {

        val xx = x * x
        val yy = y * y
        val zz = z * z
        val xy = x * y
        val xz = x * z
        val yz = y * z
        val wx = w * x
        val wy = w * y
        val wz = w * z

        // OpenGL column-major (等价 rotateM)
        m[0]  = 1f - 2f * (yy + zz)
        m[1]  = 2f * (xy + wz)
        m[2]  = 2f * (xz - wy)
        m[3]  = 0f

        m[4]  = 2f * (xy - wz)
        m[5]  = 1f - 2f * (xx + zz)
        m[6]  = 2f * (yz + wx)
        m[7]  = 0f

        m[8]  = 2f * (xz + wy)
        m[9]  = 2f * (yz - wx)
        m[10] = 1f - 2f * (xx + yy)
        m[11] = 0f

        m[12] = 0f
        m[13] = 0f
        m[14] = 0f
        m[15] = 1f
    }

    companion object

}
