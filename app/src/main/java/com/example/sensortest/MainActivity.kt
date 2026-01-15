package com.example.sensortest;

import IMUController
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener;
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Typeface
import android.opengl.GLSurfaceView
import android.util.Log

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sm: SensorManager
    private lateinit var container: LinearLayout

    private lateinit var tvPitch: TextView
    private lateinit var tvRoll: TextView

    private val sensorViews = mutableMapOf<Sensor, TextView>()

    // 稳定显示相关
    private val UI_INTERVAL_MS = 200L
    private var lastUiTime = 0L

    private val buffers = mutableMapOf<Sensor, MutableList<FloatArray>>()
    private val AVG_WINDOW = 1

    private var gx = 0f
    private var gy = 0f
    private var gz = 0f

    private val ALPHA = 0.9f   // 0.9 ~ 0.98

    private var gpitch = 0f
    private var groll = 0f

    private var pitch = 0f
    private var roll = 0f
    private var lastTimestamp = 0L

    private val K = 0.9f

    private val virtualVector = VirtualRotationVector()

    private lateinit var textPitch: TextView
    private lateinit var textRoll: TextView
    private lateinit var textYaw: TextView
    private lateinit var textRV: TextView

    private lateinit var glView:OrientationGLSurfaceView
    private lateinit var renderer: OrientationRenderer
    //private lateinit var sensorCtl: SensorController
    private lateinit var sensorCtl: IMUController

    private val sensorTypeList = listOf(
        Sensor.TYPE_ACCELEROMETER,
        Sensor.TYPE_GYROSCOPE,
        Sensor.TYPE_MAGNETIC_FIELD,
        Sensor.TYPE_LIGHT,
        Sensor.TYPE_PROXIMITY,
        Sensor.TYPE_PRESSURE,
        27,
        57, //COLOR_TEMP

    )

    private fun accAngle(ax: Float, ay: Float, az: Float): Pair<Float, Float> {
        val pitch = Math.atan2(
            -ax.toDouble(),
            Math.sqrt((ay * ay + az * az).toDouble())
        )
        val roll = Math.atan2(
            ay.toDouble(),
            az.toDouble()
        )

        return Pair(
            Math.toDegrees(pitch).toFloat(),
            Math.toDegrees(roll).toFloat()
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        sm = getSystemService(SENSOR_SERVICE) as SensorManager
        container = findViewById(R.id.container)

        //tvPitch = findViewById(R.id.textView)
        textPitch = findViewById(R.id.textPitch)
        textRoll = findViewById(R.id.textRoll)
        textYaw = findViewById(R.id.textYaw)
        textRV = findViewById(R.id.textRV)
        glView = findViewById<OrientationGLSurfaceView>(R.id.glView)
        renderer = OrientationRenderer()
        glView.setEGLContextClientVersion(2)
        glView.setRenderer(renderer)
        glView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        sensorCtl = IMUController(
            glView,
            renderer)
//        sensorCtl = SensorController(
//            glView,
//            renderer)

        val sensorList = sm.getSensorList(Sensor.TYPE_ALL)
            .sortedBy { it.type }

        for (s in sensorList) {
            if(s.type in sensorTypeList) {
                // 标题
                val title = TextView(this).apply {
                    text = "${s.name}  (type=${s.type})"
                    textSize = 15f
                    setPadding(8, 16, 8, 4)
                    setTypeface(null, Typeface.BOLD)
                }

                // 数值
                val value = TextView(this).apply {
                    textSize = 14f
                    setPadding(16, 4, 8, 8)
                }

                container.addView(title)
                container.addView(value)

                sensorViews[s] = value
                buffers[s] = ArrayDeque()
            }
        }
    }
    override fun onResume() {
        super.onResume()
        for (s in sensorViews.keys) {
            sm.registerListener(
                this,
                s,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        sensorCtl.start()
    }

    override fun onPause() {
        super.onPause()
        sm.unregisterListener(this)
        sensorCtl.stop()
    }

    override fun onSensorChanged(e: SensorEvent) {

        val buf = buffers[e.sensor] ?: return
        buf.add(e.values.clone())
        if (buf.size > AVG_WINDOW) buf.removeAt(0)

        val now = System.currentTimeMillis()
        if (now - lastUiTime < UI_INTERVAL_MS) return
        lastUiTime = now

        when (e.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> virtualVector.updateAccelerometer(e.values[0], e.values[1], e.values[2])
            Sensor.TYPE_GYROSCOPE -> virtualVector.updateGyroscope(e.values[0], e.values[1], e.values[2], e.timestamp)
            Sensor.TYPE_MAGNETIC_FIELD -> virtualVector.updateMagnetometer(e.values[0], e.values[1], e.values[2])
        }

        // 获取 Android Rotation Vector 格式
        val rotationVector = virtualVector.getRotationVector()
        val orientation = virtualVector.rotationSensor.getOrientation()

        // 更新 UI
        textPitch.text = "Pitch: %.2f°".format(orientation.first)
        textRoll.text = "Roll: %.2f°".format(orientation.second)
        textYaw.text = "Yaw: %.2f°".format(orientation.third)
        textRV.text = "Rotation Vector: %.4f, %.4f, %.4f".format(
            rotationVector[0], rotationVector[1], rotationVector[2]
        )

        //glView.updateRotation(orientation.first, orientation.second, orientation.third)
        updateUi()
    }
    private fun updateGravity(ax: Float, ay: Float, az: Float) {
        gx = ALPHA * gx + (1 - ALPHA) * ax
        gy = ALPHA * gy + (1 - ALPHA) * ay
        gz = ALPHA * gz + (1 - ALPHA) * az
    }
    private fun calcAngleFromGravity(): Pair<Float, Float> {
        val pitch = Math.atan2(
            -gx.toDouble(),
            Math.sqrt((gy * gy + gz * gz).toDouble())
        )

        val roll = Math.atan2(
            gy.toDouble(),
            gz.toDouble()
        )

        return Pair(
            Math.toDegrees(pitch).toFloat(),
            Math.toDegrees(roll).toFloat()
        )
    }

    private fun sensorUnit(sensor: Sensor): String {
        return when (sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> "m/s²"
            Sensor.TYPE_GYROSCOPE -> "rad/s"
            Sensor.TYPE_MAGNETIC_FIELD -> "μT"
            Sensor.TYPE_LIGHT -> "lx"
            Sensor.TYPE_PRESSURE -> "hPa"
            Sensor.TYPE_PROXIMITY -> "cm"
            else -> ""  // 自定义 / 虚拟 sensor 可留空
        }
    }

    private fun updateUi() {

        for ((sensor, tv) in sensorViews) {

            val buf = buffers[sensor] ?: continue
            if (buf.isEmpty()) continue

            val dim = if (buf.first().size > 3) 3 else buf.first().size
            val sum = FloatArray(dim)

            for (v in buf) {
                for (i in 0 until dim) {
                    sum[i] += v[i]
                }
            }

            val n = buf.size
            val sb = StringBuilder()
            val unit = sensorUnit(sensor)

            for (i in 0 until dim) {
                sb.append(
                    String.format(
                        "value[%d]: %.4f %s\n",
                        i,
                        sum[i] / n,
                        unit
                    )
                )
            }

            tv.text = sb.toString()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
