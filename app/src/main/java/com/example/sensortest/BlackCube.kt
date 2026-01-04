package com.example.sensortest

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class BlackCube : Cube {

    // 每个顶点：XYZ + RGBA
    private val STRIDE = 7 * 4

    private val vertexData = floatArrayOf(
        // Front (red)
        -1f,-1f, 1f, 1f,0f,0f,1f,
        1f,-1f, 1f, 1f,0f,0f,1f,
        1f, 1f, 1f, 1f,0f,0f,1f,
        -1f,-1f, 1f, 1f,0f,0f,1f,
        1f, 1f, 1f, 1f,0f,0f,1f,
        -1f, 1f, 1f, 1f,0f,0f,1f,

        // Back (green)
        1f,-1f,-1f, 0f,1f,0f,1f,
        -1f,-1f,-1f, 0f,1f,0f,1f,
        -1f, 1f,-1f, 0f,1f,0f,1f,
        1f,-1f,-1f, 0f,1f,0f,1f,
        -1f, 1f,-1f, 0f,1f,0f,1f,
        1f, 1f,-1f, 0f,1f,0f,1f,

        // Left (blue)
        -1f,-1f,-1f, 0f,0f,1f,1f,
        -1f,-1f, 1f, 0f,0f,1f,1f,
        -1f, 1f, 1f, 0f,0f,1f,1f,
        -1f,-1f,-1f, 0f,0f,1f,1f,
        -1f, 1f, 1f, 0f,0f,1f,1f,
        -1f, 1f,-1f, 0f,0f,1f,1f,

        // Right (yellow)
        1f,-1f, 1f, 1f,1f,0f,1f,
        1f,-1f,-1f, 1f,1f,0f,1f,
        1f, 1f,-1f, 1f,1f,0f,1f,
        1f,-1f, 1f, 1f,1f,0f,1f,
        1f, 1f,-1f, 1f,1f,0f,1f,
        1f, 1f, 1f, 1f,1f,0f,1f,

        // Top (cyan)
        -1f, 1f, 1f, 0f,1f,1f,1f,
        1f, 1f, 1f, 0f,1f,1f,1f,
        1f, 1f,-1f, 0f,1f,1f,1f,
        -1f, 1f, 1f, 0f,1f,1f,1f,
        1f, 1f,-1f, 0f,1f,1f,1f,
        -1f, 1f,-1f, 0f,1f,1f,1f,

        // Bottom (magenta)
        -1f,-1f,-1f, 1f,0f,1f,1f,
        1f,-1f,-1f, 1f,0f,1f,1f,
        1f,-1f, 1f, 1f,0f,1f,1f,
        -1f,-1f,-1f, 1f,0f,1f,1f,
        1f,-1f, 1f, 1f,0f,1f,1f,
        -1f,-1f, 1f, 1f,0f,1f,1f,
    )
    private var program = 0
    private val buffer: FloatBuffer =
        ByteBuffer.allocateDirect(vertexData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(vertexData)
                position(0)
            }

    override fun initGL()
    {
        program = GLUtil.createProgram(
            """
        uniform mat4 uMVPMatrix;
        attribute vec3 aPosition;
        attribute vec4 aColor;
        varying vec4 vColor;
        void main() {
            gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
            vColor = aColor;
        }
        """.trimIndent(),
            """
        precision mediump float;
        varying vec4 vColor;
        void main() {
            gl_FragColor = vColor;
        }
        """.trimIndent()
        )
    }
    override fun draw(mvpMatrix: FloatArray) {

        GLES20.glUseProgram(program)

        val pos = GLES20.glGetAttribLocation(program, "aPosition")
        val col = GLES20.glGetAttribLocation(program, "aColor")
        val mvpLoc = GLES20.glGetUniformLocation(program, "uMVPMatrix")

        buffer.position(0)
        GLES20.glVertexAttribPointer(pos, 3, GLES20.GL_FLOAT, false, STRIDE, buffer)
        GLES20.glEnableVertexAttribArray(pos)

        buffer.position(3)
        GLES20.glVertexAttribPointer(col, 4, GLES20.GL_FLOAT, false, STRIDE, buffer)
        GLES20.glEnableVertexAttribArray(col)

        GLES20.glUniformMatrix4fv(mvpLoc, 1, false, mvpMatrix, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36)

        GLES20.glDisableVertexAttribArray(pos)
        GLES20.glDisableVertexAttribArray(col)
    }
}
