package com.example.sensortest

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

class OrientationRenderer : GLSurfaceView.Renderer {

    private var TAG = "OrientationRenderer"

    private val mModelMatrix = FloatArray(16)
    private val mViewMatrix = FloatArray(16)
    private val mProjMatrix = FloatArray(16)
    private val mMVPMatrix = FloatArray(16)
    private val cube = SimpleCube()

    @Volatile
    private var pitch = 0f
    @Volatile
    private var roll = 0f
    @Volatile
    private var yaw = 0f

    private val q = Quaternion()

    private val qStable = Quaternion()

    // 稳定系数：0.05～0.2 推荐
    private val smoothFactor = 0.1f

    fun updateQuaternion(src: Quaternion) {
        q.w = src.w; q.x = src.x; q.y = src.y; q.z = src.z
    }

    fun slerp(a: Quaternion, b: Quaternion, t: Float): Quaternion {
        var cosHalfTheta =
            a.w*b.w + a.x*b.x + a.y*b.y + a.z*b.z

        if (cosHalfTheta < 0f) {
            cosHalfTheta = -cosHalfTheta
            b.w = -b.w; b.x = -b.x; b.y = -b.y; b.z = -b.z
        }

        if (cosHalfTheta > 0.9995f) {
            return Quaternion(
                a.w + t*(b.w - a.w),
                a.x + t*(b.x - a.x),
                a.y + t*(b.y - a.y),
                a.z + t*(b.z - a.z)
            ).also { it.normalize() }
        }

        val halfTheta = kotlin.math.acos(cosHalfTheta)
        val sinHalfTheta = kotlin.math.sqrt(1f - cosHalfTheta*cosHalfTheta)

        val ratioA = kotlin.math.sin((1f - t) * halfTheta) / sinHalfTheta
        val ratioB = kotlin.math.sin(t * halfTheta) / sinHalfTheta

        return Quaternion(
            a.w*ratioA + b.w*ratioB,
            a.x*ratioA + b.x*ratioB,
            a.y*ratioA + b.y*ratioB,
            a.z*ratioA + b.z*ratioB
        )
    }

    init {

    }
    fun setRotation(pitch: Float, roll: Float, yaw: Float) {
        this.pitch = pitch
        this.roll = roll
        this.yaw = yaw
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        // 摄像机位置在 z=7，看向原点
        Matrix.setLookAtM(mViewMatrix, 0,
            0f, 0f, 7f,
            0f, 0f, 0f,
            0f, 1f, 0f
        )
        cube.initGL()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height
        Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 20f)
    }

    override fun onDrawFrame(gl: GL10?) {

        val qs = slerp(qStable, q, smoothFactor)

        qStable.w = qs.w; qStable.x = qs.x
        qStable.y = qs.y; qStable.z = qs.z

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        //Matrix.setIdentityM(mModelMatrix, 0)

        qStable.toMatrix(mModelMatrix)
        //qStable.toGLMatrix(mModelMatrix)
        // 顺序 Roll -> Pitch -> Yaw
//        Matrix.rotateM(mModelMatrix, 0, roll, 0f, 0f, 1f)
//        Matrix.rotateM(mModelMatrix, 0, pitch, 1f, 0f, 0f)
//        Matrix.rotateM(mModelMatrix, 0, yaw, 0f, 1f, 0f)

        // MVP = Projection * View * Model
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0)
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0)

        cube.draw(mMVPMatrix)
    }
}
