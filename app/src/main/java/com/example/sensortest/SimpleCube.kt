package com.example.sensortest

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class SimpleCube : Cube {

    private val vertices = floatArrayOf(
        -1f, -1f, -1f,  // 0
        1f, -1f, -1f,  // 1
        1f,  1f, -1f,  // 2
        -1f,  1f, -1f,  // 3
        -1f, -1f,  1f,  // 4
        1f, -1f,  1f,  // 5
        1f,  1f,  1f,  // 6
        -1f,  1f,  1f   // 7
    )

    private val indices = shortArrayOf(
        0,1,2, 0,2,3,   // 后
        4,5,6, 4,6,7,   // 前
        0,4,7, 0,7,3,   // 左
        1,5,6, 1,6,2,   // 右
        3,2,6, 3,6,7,   // 上
        0,1,5, 0,5,4    // 下
    )

    private val colors = floatArrayOf(
        1f,0f,0f,1f,  // 0
        0f,1f,0f,1f,  // 1
        0f,0f,1f,1f,  // 2
        1f,1f,0f,1f,  // 3
        1f,0f,1f,1f,  // 4
        0f,1f,1f,1f,  // 5
        1f,0.5f,0f,1f,// 6
        0.5f,0f,1f,1f // 7
    )

    private val vertexBuffer: FloatBuffer
    private val colorBuffer: FloatBuffer
    private val indexBuffer: ShortBuffer

    private var program = 0
    private val vertexShaderCode =
        "uniform mat4 uMVPMatrix;" +
                "attribute vec4 vPosition;" +
                "attribute vec4 aColor;" +
                "varying vec4 vColor;" +
                "void main() { gl_Position = uMVPMatrix * vPosition; vColor = aColor; }"

    private val fragmentShaderCode =
        "precision mediump float;" +
                "varying vec4 vColor;" +
                "void main() { gl_FragColor = vColor; }"

    init {
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(vertices)
                position(0)
            }

        colorBuffer = ByteBuffer.allocateDirect(colors.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(colors)
                position(0)
            }

        indexBuffer = ByteBuffer.allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder()).asShortBuffer().apply {
                put(indices)
                position(0)
            }

    }
    override fun initGL()
    {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }
    }
    override fun draw(mvpMatrix: FloatArray) {

        GLES20.glUseProgram(program)

        val posHandle = GLES20.glGetAttribLocation(program, "vPosition")
        GLES20.glEnableVertexAttribArray(posHandle)
        GLES20.glVertexAttribPointer(posHandle,3,GLES20.GL_FLOAT,false,12,vertexBuffer)

        val colorHandle = GLES20.glGetAttribLocation(program, "aColor")
        GLES20.glEnableVertexAttribArray(colorHandle)
        GLES20.glVertexAttribPointer(colorHandle,4,GLES20.GL_FLOAT,false,16,colorBuffer)

        val mvpHandle = GLES20.glGetUniformLocation(program,"uMVPMatrix")
        GLES20.glUniformMatrix4fv(mvpHandle,1,false,mvpMatrix,0)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.size, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        GLES20.glDisableVertexAttribArray(posHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }

    private fun loadShader(type:Int, code:String):Int{
        return GLES20.glCreateShader(type).also { shader->
            GLES20.glShaderSource(shader, code)
            GLES20.glCompileShader(shader)
        }
    }
}

