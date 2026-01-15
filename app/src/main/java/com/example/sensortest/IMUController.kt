import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.MotionEvent
import android.view.Surface

import com.example.sensortest.OrientationGLSurfaceView
import com.example.sensortest.OrientationRenderer
import com.example.sensortest.Quaternion

const val RAD2DEG = 180f / Math.PI.toFloat()

const val DEG2RAD = Math.PI.toFloat() / 180f

class IMUController(
    private val glView: OrientationGLSurfaceView,
    private val renderer: OrientationRenderer
) : SensorEventListener {

    private val sm = glView.context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val hasGyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null

    private val q = Quaternion()
    private val acc = FloatArray(3)
    private var lastTs = 0L
    private var rotation: Int = 1

    private var fakeYaw = 0f

    private var mPitch = 0f
    private var mRoll = 0f
    private var mYaw = 0f

    fun start() {
        sm.registerListener(this,
            sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_GAME)

        sm.registerListener(this,
            sm.getDefaultSensor(27),
            SensorManager.SENSOR_DELAY_GAME)

        if (hasGyro) {
            sm.registerListener(this,
                sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_GAME)
        }

        glView.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_MOVE && !hasGyro) {
                fakeYaw += e.historySize.takeIf { it > 0 }?.let {
                    (e.x - e.getHistoricalX(0)) * 0.2f
                } ?: 0f
            }
            true
        }

    }

    fun stop() = sm.unregisterListener(this)

    fun quatFromAcc(ax: Float, ay: Float, az: Float): Quaternion {
        val norm = kotlin.math.sqrt(ax*ax + ay*ay + az*az)
        if (norm < 1e-6f) return Quaternion()

        val x = ax / norm
        val y = ay / norm
        val z = az / norm

        val pitch = kotlin.math.atan2(-x, kotlin.math.sqrt(y*y + z*z))
        val roll = kotlin.math.atan2(y, z)

        mPitch = Math.toDegrees(pitch.toDouble()).toFloat()
        mRoll = Math.toDegrees(roll.toDouble()).toFloat()
        mYaw = 0f;

//        val qx = fromAxisAngle(1f, 0f, 0f, pitch * RAD2DEG)
//        val qz = fromAxisAngle(0f, 0f, 1f, roll  * RAD2DEG)
//
//        return qx.multiply(qz)

        return quatFromEuler(pitch, roll, 0f)
    }

    fun quatFromEuler(pitch: Float, roll: Float, yaw: Float): Quaternion {
        val cy = kotlin.math.cos(yaw * 0.5f)
        val sy = kotlin.math.sin(yaw * 0.5f)
        val cp = kotlin.math.cos(pitch * 0.5f)
        val sp = kotlin.math.sin(pitch * 0.5f)
        val cr = kotlin.math.cos(roll * 0.5f)
        val sr = kotlin.math.sin(roll * 0.5f)

        return Quaternion(
            cr*cp*cy + sr*sp*sy,
            sr*cp*cy - cr*sp*sy,
            cr*sp*cy + sr*cp*sy,
            cr*cp*sy - sr*sp*cy
        )
    }

    fun quatFromYaw(deg: Float): Quaternion {
        val rad = Math.toRadians(deg.toDouble()).toFloat()
        val h = rad * 0.5f
        return Quaternion(
            kotlin.math.cos(h),
            0f,
            0f,
            kotlin.math.sin(h)
        )
    }

    override fun onSensorChanged(e: SensorEvent) {
        when (e.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {

                acc[0] = e.values[0]
                acc[1] = e.values[1]
                acc[2] = e.values[2]

                // ⭐ 无 gyro → 直接用 acc 算姿态
                if (!hasGyro) {
                    val qa = quatFromAcc(acc[0], acc[1], acc[2])
                    q.w = qa.w; q.x = qa.x; q.y = qa.y; q.z = qa.z
                }
            }

            Sensor.TYPE_GYROSCOPE -> {
                if (!hasGyro) return

                if (lastTs != 0L) {
                    val dt = (e.timestamp - lastTs) * 1e-9f
                    q.integrateGyro(e.values[0], e.values[1], e.values[2], dt)
                    q.correctWithAccel(acc[0], acc[1], acc[2])
                }
                lastTs = e.timestamp
            }
            27 -> {
                //rotation = e.values[0].toInt()
            }
        }

        updateFakeYaw(0.016f) // ~60fps

        val qYaw = quatFromYaw(fakeYaw).multiply(q)

        var qOut = applyScreenRotation(qYaw)

        glView.queueEvent {
            //renderer.setRotation(mPitch,mRoll,mYaw)
            renderer.updateQuaternion(qOut)
        }
    }

    private fun updateFakeYaw(dt: Float) {
        fakeYaw *= dt//0.98f   // 每帧衰减
    }

    fun screenRotationToYaw(rotation: Int): Float {
        return when (rotation) {
            Surface.ROTATION_0   -> 0f
            Surface.ROTATION_90  -> -90f
            Surface.ROTATION_180 -> -180f
            Surface.ROTATION_270 -> -270f
            else -> 0f
        }
    }

    fun quatFromZRotation(deg: Float): Quaternion {
        val rad = Math.toRadians(deg.toDouble()).toFloat()
        val half = rad * 0.5f
        return Quaternion(
            kotlin.math.cos(half),
            0f,
            0f,
            kotlin.math.sin(half)
        )
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

    private fun applyScreenRotation(src: Quaternion): Quaternion {

        val yaw = screenRotationToYaw(rotation)
        val qScreen = quatFromZRotation(yaw)

        // 注意顺序：屏幕补偿 * 姿态
        return qScreen.multiply(src)
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

