package com.example.sensortest

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

abstract interface Cube {
    abstract fun initGL()
    abstract fun draw(mvpMatrix: FloatArray)
}
