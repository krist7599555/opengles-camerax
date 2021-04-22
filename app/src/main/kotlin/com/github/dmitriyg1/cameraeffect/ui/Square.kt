package com.github.dmitriyg1.cameraeffect.ui

import android.opengl.GLES20
import android.opengl.GLES20.*
import android.opengl.GLException
import android.opengl.Matrix
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

fun floatarrayToBuffer(fa: FloatArray): FloatBuffer {
  return ByteBuffer
    .allocateDirect(fa.size * 4)
    .order(ByteOrder.nativeOrder())
    .asFloatBuffer()
    .apply {
      put(fa)
      position(0)
    }
}

class Square {
  private val vertices = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
  private val textureVertices = floatArrayOf(0f, 1f, 1f, 1f, 0f, 0f, 1f, 0f)

  private val arrow = floatArrayOf(
    -.5f, -.5f, .5f, //
    -.5f, .5f, .5f, //
    .5f, .5f, .5f, //
    .5f, -.5f, .5f, //
    -.5f, -.5f, -.5f, //
    -.5f, .5f, -.5f, //
    .5f, .5f, -.5f, //
    .5f, -.5f, -.5f  //
  )

  private val projectionMatrix = FloatArray(16).apply {
//    val camera = FloatArray(16)
//    val project = FloatArray(16)
//    Matrix.setIdentityM(camera, 0)
//    Matrix.perspectiveM(project, 0, 30f, 1f / 1f, 0.1f, 10f)
//    Matrix.multiplyMM(this, 0, project, 0, camera, 0);
//    Matrix.setIdentityM(this, 0)
  }

  private lateinit var verticesBuffer: FloatBuffer
  private lateinit var textureBuffer: FloatBuffer
  private lateinit var arrowBuffer: FloatBuffer
  private lateinit var projectionMatrixBuffer : FloatBuffer

  private var vertexShader: Int = 0
  private var fragmentShader: Int = 0
  private var program: Int = 0


  private val vertexShaderCode =
    "#version 100\n" +
    "precision mediump float;" +
      "precision mediump int;" +
    "uniform int uMode;" +
    "uniform mat4 uProjectionMatrix;" +
    "attribute vec4 aPosition;" +
    "attribute vec2 aTexPosition;" +
    "varying vec2 vTexPosition;" +
    "mat4 identity = mat4(1.0,0.0,0.0,0.5, 0.0,1.0,0.0,0.0, 0.0,0.0,1.0,0.0, 0.0,0.0,0.0,1.0);" +
    "void main() {" +
    "  if (uMode == 1) {" +
    "    gl_Position = aPosition;" +
    "  } else {" +
//    "    gl_Position = vec4(aPosition.x, aPosition.y, aPosition.z, 1.0);" +
//      "    gl_Position = aPosition;" +
//      "    gl_Position = vec4(aPosition.xyz, 1.0);" +
//    "    gl_Position = vec4(-1.0,1.0,1.0,1.0) * vec4(aPosition.xyz, 1.0);" + // flip y
      "    gl_Position = identity * vec4(aPosition.xyz, 1.0);" +
    "  }" +
    "  vTexPosition = aTexPosition;" +
    "}"

  private val fragmentShaderCode =
    "#version 100\n" +
    "precision mediump float;" +
      "precision mediump int;" +
    "uniform int uMode;" +
    "uniform sampler2D uTexture;" +
    "varying vec2 vTexPosition;" +
    "void main() {" +
    "  if (uMode == 1) {" +
    "    gl_FragColor = texture2D(uTexture, vTexPosition);" +
    "  } else {" +
    "    gl_FragColor = vec4(1, 0, 0, 1);" +
    "  }" +
    "}"

  init {
    initializeBuffers()
    initializeProgram()
  }

  private fun initializeBuffers() {
    var buff = ByteBuffer.allocateDirect(vertices.size * 4)
    buff.order(ByteOrder.nativeOrder())
    verticesBuffer = buff.asFloatBuffer()
    verticesBuffer.put(vertices)
    verticesBuffer.position(0)

    buff = ByteBuffer.allocateDirect(textureVertices.size * 4)
    buff.order(ByteOrder.nativeOrder())
    textureBuffer = buff.asFloatBuffer()
    textureBuffer.put(textureVertices)
    textureBuffer.position(0)

    buff = ByteBuffer.allocateDirect(arrow.size * 4)
    buff.order(ByteOrder.nativeOrder())
    arrowBuffer = buff.asFloatBuffer()
    arrowBuffer.put(arrow)
    arrowBuffer.position(0)


//    val p = FloatArray(16)
//    Matrix.setIdentityM(p, 0)
//    Matrix.transposeM(projectionMatrix, 0, p , 0);
    Matrix.setIdentityM(projectionMatrix, 0)
    buff = ByteBuffer.allocateDirect(projectionMatrix.size * 4)
    buff.order(ByteOrder.nativeOrder())
    projectionMatrixBuffer = buff.asFloatBuffer()
    projectionMatrixBuffer.put(projectionMatrix)
    projectionMatrixBuffer.position(0)
  }

  private fun initializeProgram() {
    val result = IntArray(1)

    vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
    GLES20.glShaderSource(vertexShader, vertexShaderCode)
    GLES20.glCompileShader(vertexShader)
    GLES20.glGetShaderiv(vertexShader, GLES20.GL_COMPILE_STATUS, result, 0)
    if (result[0] == GL_FALSE) {
      Log.e(
        "CameraGLRendererBase", "Could not compile vertex shader: " + GLES20.glGetShaderInfoLog(
          vertexShader
        )
      );
      GLES20.glDeleteShader(vertexShader);
      throw GLException(result[0], "Could not compile vertex shader")
    }

    fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)
    GLES20.glShaderSource(fragmentShader, fragmentShaderCode)
    GLES20.glCompileShader(fragmentShader)
    GLES20.glGetShaderiv(fragmentShader, GLES20.GL_COMPILE_STATUS, result, 0)
    if (result[0] == GL_FALSE) {
      Log.e(
        "CameraGLRendererBase", "Could not compile fragment shader:" + GLES20.glGetShaderInfoLog(
          fragmentShader
        )
      );
      GLES20.glDeleteShader(vertexShader)
      GLES20.glDeleteShader(fragmentShader)
      throw GLException(result[0], "Could not compile fragment shader")
    }

    program = GLES20.glCreateProgram()
    GLES20.glAttachShader(program, vertexShader)
    GLES20.glAttachShader(program, fragmentShader)

    GLES20.glLinkProgram(program)
    GLES20.glGetProgramiv(program, GL_LINK_STATUS, result, 0);
    if (result[0] === GL_FALSE) {
      val message = "Link program fail" + GLES20.glGetProgramInfoLog(program)
      Log.e("CameraGLRendererBase", message)
      throw GLException(result[0], message)
    }

    GLES20.glValidateProgram(program)
    GLES20.glGetProgramiv(program, GLES20.GL_VALIDATE_STATUS, result, 0)
    if (result[0] === GL_FALSE) {
      val message = if (!GLES20.glIsProgram(program)) "Program handle deprecated!" else "Program do not validated!"
      throw GLException(result[0], message)
    }
  }

  fun draw(texture: Int) {
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    GLES20.glUseProgram(program)
    GLES20.glDisable(GLES20.GL_BLEND)

    val modeHandle = GLES20.glGetUniformLocation(program, "uMode")
    val projectionMatrixHandle = GLES20.glGetUniformLocation(program, "uProjectionMatrix")
    val positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
    val textureHandle = GLES20.glGetUniformLocation(program, "uTexture")
    val texturePositionHandle = GLES20.glGetAttribLocation(program, "aTexPosition")

    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

    GLES20.glUniform1i(modeHandle, 1) // texture

    GLES20.glVertexAttribPointer(texturePositionHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer)
    GLES20.glEnableVertexAttribArray(texturePositionHandle)

    GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
    GLES20.glUniform1i(textureHandle, 0)

    GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, verticesBuffer)
    GLES20.glEnableVertexAttribArray(positionHandle)

    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

    GLES20.glDisableVertexAttribArray(positionHandle)

    GLES20.glUniform1i(modeHandle, 2) // shader

    println(projectionMatrixBuffer)
    GLES20.glUniformMatrix4fv(projectionMatrixHandle, 0, false, projectionMatrixBuffer)

    GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, arrowBuffer)
    GLES20.glEnableVertexAttribArray(positionHandle)

    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

    GLES20.glDisableVertexAttribArray(positionHandle)

  }
}
