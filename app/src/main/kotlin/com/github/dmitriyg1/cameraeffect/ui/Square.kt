package com.github.dmitriyg1.cameraeffect.ui

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLES20.*
import android.opengl.GLException
import android.opengl.Matrix
import android.util.Log
import java.io.BufferedReader
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

class Square(private val context: Context) {
  private val vertices = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
  private val textureVertices = floatArrayOf(0f, 1f, 1f, 1f, 0f, 0f, 1f, 0f)
  private lateinit var arrowModel: Model
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

  private val modelMatrix = FloatArray(16).apply {
    Matrix.setIdentityM(this, 0)
    Matrix.rotateM(this, 0, 90f, 1f, 0f, 0f)
  }
  private val cameraMatrix = FloatArray(16).apply {
    Matrix.setLookAtM(this, 0, 2f, 4f, 1.2f, 0f, 0f, 0f, 0f, 0f, 2f);
  }
  private val projectionMatrix = FloatArray(16).apply {
    Matrix.perspectiveM(this, 0, 30f, 1f / 1f, 0f, 10f)
  }

  private val _modelView = FloatArray(16)
  private val _modelViewProjection = FloatArray(16)
  private var _modelViewProjectionBuffer = floatarrayToBuffer(_modelViewProjection)
  private fun calculateMvpBuffer(): FloatBuffer {
    Matrix.multiplyMM(_modelView, 0, cameraMatrix, 0, modelMatrix, 0);
    Matrix.multiplyMM(_modelViewProjection, 0, projectionMatrix, 0, _modelView, 0);
    _modelViewProjectionBuffer = floatarrayToBuffer(_modelViewProjection)
    return _modelViewProjectionBuffer
  }

  private lateinit var verticesBuffer: FloatBuffer
  private lateinit var textureBuffer: FloatBuffer
  private lateinit var arrowBuffer: FloatBuffer
  private lateinit var indicesBuffer : ShortBuffer

//  private lateinit var arrowBuffer: FloatBuffer

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
    "attribute vec3 aNormal;" +
    "varying vec2 vTexPosition;" +
    "varying vec3 vNormal;" +
//    "mat4 identity = mat4(1.0,0.0,0.0,0.0, 0.0,1.0,0.0,0.0, 0.0,0.0,1.0,0.0, 0.0,0.0,0.0,1.0);" +
    "void main() {" +
    "  if (uMode == 1) {" +
    "    gl_Position = aPosition;" +
    "  } else if (uMode == 2) {" +
      "  gl_Position = uMVP * aPosition;" +
    "  } else if (uMode == 3) {" +
    "    gl_Position = uMVP * aPosition;" +
    "    vNormal = aNormal;" +
      // https://learnopengl.com/Lighting/Basic-lighting
      // Normal = mat3(transpose(inverse(model))) * aNormal;
    "  }" +
    "  vTexPosition = aTexPosition;" +
    "    vNormal = aNormal;" +
    "}"

  private val fragmentShaderCode =
    "#version 100\n" +
    "precision mediump float;" +
    "precision mediump int;" +
    "uniform int uMode;" +
    "uniform sampler2D uTexture;" +
    "varying vec2 vTexPosition;" +
    "varying vec3 vNormal;" +
    "vec3 lightDir = normalize(vec3(3.0, 2.0, -60.0));" +
    "vec3 lightColor = vec3(1.0, 1.0, 1.0);" +
    "vec3 objectColor = vec3(0.8, 0.2, 0.6);" +
    "float ambientStrength = 1.0;" +
    "void main() {" +
    "  if (uMode == 1) {" +
    "    gl_FragColor = texture2D(uTexture, vTexPosition);" +
    "  } else if (uMode == 2) {" +
    "    gl_FragColor = vec4(1, 0, 0, 1);" +
    "  } else if (uMode == 3) {" +
    "    float ambientStrength = 0.3;" +
    "    vec3 ambient = ambientStrength * lightColor;" +
    "    float diff = max(dot(vNormal, lightDir), 0.0);" +
    "    float diffuseStrength = 1.0;" +
    "    vec3 diffuse = diff * lightColor * diffuseStrength;" +
    "    vec3 result = (ambient + diffuse) * objectColor;" +
    "    gl_FragColor = vec4(result, 1.0);" +
    "    " +
    "  }" +
    "}"

  init {
    initializeBuffers()
    initializeProgram()
  }

  private fun initializeBuffers() {
    arrowModel = Model.fromOBJ(
      context.assets
        .open("arrow/arrowk.obj")
        .bufferedReader()
        .use(BufferedReader::readText)
    )

    verticesBuffer  = floatarrayToBuffer(vertices)
    textureBuffer   = floatarrayToBuffer(textureVertices)
    arrowBuffer     = floatarrayToBuffer(arrowModel.positions)
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

  var drawCount = 0;
  fun draw(texture: Int) {
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    GLES20.glUseProgram(program)
    GLES20.glEnable(GLES20.GL_BLEND)

    val modeHandle = GLES20.glGetUniformLocation(program, "uMode")
    val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVP")
    val positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
    val textureHandle = GLES20.glGetUniformLocation(program, "uTexture")
    val texturePositionHandle = GLES20.glGetAttribLocation(program, "aTexPosition")
    val normalHandle = glGetAttribLocation(program, "aNormal");

//  DEBUG CHANGE

    Matrix.rotateM(modelMatrix, 0, 1f, 0f, 1f, 0f);

//  SETUP

    glEnableVertexAttribArray(texturePositionHandle)
    glEnableVertexAttribArray(positionHandle)
    glEnableVertexAttribArray(normalHandle)

    glClearColor(0f, 1f, 1f, 1f);
    glClear(GL_COLOR_BUFFER_BIT)
    glEnable(GL_CULL_FACE);

//  DRAW CAMERA

    glUniform1i(modeHandle, 1) // mode
    glVertexAttribPointer(texturePositionHandle, 2, GL_FLOAT, false, 0, textureBuffer)

    glActiveTexture(GL_TEXTURE0)
    glBindTexture(GL_TEXTURE_2D, texture)
    glUniform1i(textureHandle, 0)

    glVertexAttribPointer(positionHandle, 2, GL_FLOAT, false, 0, verticesBuffer)
    glUniformMatrix4fv(mvpMatrixHandle, 1, false, calculateMvpBuffer()) // use mvp
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)

//  DRAW ARROW MODEL

    glUniform1i(modeHandle, 3) // mode

    glClear(GL_DEPTH_BUFFER_BIT)

    arrowModel.bindPositionAttributeTo(positionHandle)
    arrowModel.bindNormalAttributeTo(normalHandle);
    arrowModel.bindTextureAttributeTo(texturePositionHandle)
    arrowModel.draw()

//  CLEANUP

    glDisableVertexAttribArray(positionHandle)
    glDisableVertexAttribArray(normalHandle)
    glDisableVertexAttribArray(texturePositionHandle)

  }
}
