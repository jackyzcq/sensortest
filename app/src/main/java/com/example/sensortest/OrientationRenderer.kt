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

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        Matrix.setIdentityM(mModelMatrix, 0)
        // 顺序 Roll -> Pitch -> Yaw
        Matrix.rotateM(mModelMatrix, 0, roll, 0f, 0f, 1f)
        Matrix.rotateM(mModelMatrix, 0, pitch, 1f, 0f, 0f)
        Matrix.rotateM(mModelMatrix, 0, yaw, 0f, 1f, 0f)

        // MVP = Projection * View * Model
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0)
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0)

        cube.draw(mMVPMatrix)
    }
}
