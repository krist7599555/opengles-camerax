package com.github.dmitriyg1.cameraeffect.ui

import android.opengl.GLES20
import android.opengl.GLES20.*
import android.opengl.GLException
import android.opengl.Matrix
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

private fun floatarrayToBuffer(fa: FloatArray): FloatBuffer {
  return ByteBuffer
    .allocateDirect(fa.size * 4)
    .order(ByteOrder.nativeOrder())
    .asFloatBuffer()
    .apply {
      put(fa)
      position(0)
    }
}

private fun shortarrayToBuffer(fa: ShortArray): ShortBuffer {
  return ByteBuffer
    .allocateDirect(fa.size * Short.SIZE_BYTES)
    .order(ByteOrder.nativeOrder())
    .asShortBuffer()
    .apply {
      put(fa)
      position(0)
    }
}

class Square {
  private val vertices = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
  private val textureVertices = floatArrayOf(0f, 1f, 1f, 1f, 0f, 0f, 1f, 0f)

  private val arrow = floatArrayOf(
    -.5f, -.5f, .5f, // F TL
    .5f, -.5f, .5f, // F TR
    -.5f, -.5f, -.5f, // F BL
    .5f, -.5f, -.5f, // F BR
    -.5f, .5f, .5f, // B TL
    .5f, .5f, .5f, // B TR
    -.5f, .5f, -.5f, // B BL
    .5f, .5f, -.5f // B BR

  )

  private val indices = shortArrayOf(
    0, 1, 2, 2, 1, 3, // F
    3, 1, 5, 3, 5, 7, // R
    0, 4, 5, 0, 5, 1, // T
    6, 4, 0, 6, 0, 2 // L
  );

  private val mvpMatrix = FloatArray(16).apply {
    val model = FloatArray(16)
    val camera = FloatArray(16)
    val project = FloatArray(16)
    Matrix.setIdentityM(model, 0)
//    Matrix.scaleM(model, 0, .5f, .5f, .5f);
    Matrix.setLookAtM(camera, 0, 2f, 4f, 1.2f, 0f, 0f, 0f, 0f, 0f, 2f);
    Matrix.perspectiveM(project, 0, 30f, 1f / 1f, 0.1f, 10f)

    val modelView = FloatArray(16)
    Matrix.multiplyMM(modelView, 0, camera, 0, model, 0);
    Matrix.multiplyMM(this, 0, project, 0, modelView, 0);
//    Matrix.setIdentityM(this, 0);
  }

  private lateinit var verticesBuffer: FloatBuffer
  private lateinit var textureBuffer: FloatBuffer
  private lateinit var arrowBuffer: FloatBuffer
  private lateinit var mvpMatrixBuffer : FloatBuffer
  private lateinit var indicesBuffer : ShortBuffer

  private var vertexShader: Int = 0
  private var fragmentShader: Int = 0
  private var program: Int = 0


  private val vertexShaderCode =
    "#version 100\n" +
    "precision mediump float;" +
      "precision mediump int;" +
    "uniform int uMode;" +
    "uniform mat4 uMVP;" +
    "attribute vec4 aPosition;" +
    "attribute vec2 aTexPosition;" +
    "varying vec2 vTexPosition;" +
    "mat4 identity = mat4(1.0,0.0,0.0,0.0, 0.0,1.0,0.0,0.0, 0.0,0.0,1.0,0.0, 0.0,0.0,0.0,1.0);" +
    "void main() {" +
    "  if (uMode == 1) {" +
    "    gl_Position = aPosition;" +
    "  } else {" +
//    "    gl_Position = vec4(aPosition.x, aPosition.y, aPosition.z, 1.0);" +
//      "    gl_Position = aPosition;" +
//      "    gl_Position = vec4(aPosition.xyz, 1.0);" +
//    "    gl_Position = vec4(-1.0,1.0,1.0,1.0) * vec4(aPosition.xyz, 1.0);" + // flip y
      "    gl_Position = uMVP * aPosition;" +
//      "    gl_Position = identity * vec4(aPosition.xyz, 1.0);" +
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

    verticesBuffer  = floatarrayToBuffer(vertices)
    textureBuffer   = floatarrayToBuffer(textureVertices)
    arrowBuffer     = floatarrayToBuffer(arrow)
    mvpMatrixBuffer = floatarrayToBuffer(mvpMatrix)
    indicesBuffer   = shortarrayToBuffer(indices)
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
    val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVP")
    val positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
    val textureHandle = GLES20.glGetUniformLocation(program, "uTexture")
    val texturePositionHandle = GLES20.glGetAttribLocation(program, "aTexPosition")

    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    glDisable(GL_CULL_FACE);

    GLES20.glUniform1i(modeHandle, 1) // texture

    GLES20.glVertexAttribPointer(texturePositionHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer)
    GLES20.glEnableVertexAttribArray(texturePositionHandle)

    GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
    GLES20.glUniform1i(textureHandle, 0)

    GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, verticesBuffer)
    GLES20.glEnableVertexAttribArray(positionHandle)

    Log.d("QSuare", "Ebefore uniform")
    GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrixBuffer)
    Log.d("QSuare", "After uniform")

    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

//    GLES20.glDisableVertexAttribArray(positionHandle)

    GLES20.glUniform1i(modeHandle, 2) // shader

    println(mvpMatrixBuffer)


    GLES20.glUniformMatrix4fv(mvpMatrixHandle, 0, false, mvpMatrixBuffer)

    GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, arrowBuffer)
//    GLES20.glEnableVertexAttribArray(positionHandle)

//    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)

    GLES20.glDrawElements(GL_TRIANGLES, 24, GL_UNSIGNED_SHORT, indicesBuffer)
    GLES20.glDisableVertexAttribArray(positionHandle)


    var error = GLES20.glGetError()
    while (error != GL_NO_ERROR) {
      Log.d("QSuare","Error Krist GL Drawing code $error")
      throw Exception("OPENGL is ERROR in code $error")
      error = glGetError()
    }
    Log.d("QSuare", "End Krist GL Drawing")
  }
}
