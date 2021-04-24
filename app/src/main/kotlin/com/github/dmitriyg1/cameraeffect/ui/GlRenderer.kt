package com.github.dmitriyg1.cameraeffect.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.effect.Effect
import android.media.effect.EffectContext
import android.media.effect.EffectFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.github.dmitriyg1.cameraeffect.util.toBitmap
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GlRenderer(private val context: Context, private val gLSurfaceView: GLSurfaceView) : GLSurfaceView.Renderer, ImageAnalysis.Analyzer {
  private val textures = IntArray(2)
  private var square: Square? = null

  private var effectContext: EffectContext? = null
  private var effect: Effect? = null

  private var image: Bitmap? = null

  override fun onDrawFrame(p0: GL10?) {
    generateTexture()

    if (effectContext == null) {
      effectContext = EffectContext.createWithCurrentGlContext()
    }

    effect?.release()
    val USE_EFFECT = false
    if (USE_EFFECT) {
      applyEffect()
      square?.draw(textures[1])
    } else {
      square?.draw(textures[0])
    }
  }

  override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
    GLES20.glViewport(0, 0, width, height)
    GLES20.glClearColor(0f, 0f, 0f, 1f)
    square = Square(context)
  }

  override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
  }

  private fun generateTexture() {
    GLES20.glGenTextures(2, textures, 0)
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])

    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(
      GLES20.GL_TEXTURE_2D,
      GLES20.GL_TEXTURE_WRAP_S,
      GLES20.GL_CLAMP_TO_EDGE
    )
    GLES20.glTexParameteri(
      GLES20.GL_TEXTURE_2D,
      GLES20.GL_TEXTURE_WRAP_T,
      GLES20.GL_CLAMP_TO_EDGE
    )

    image?.run {
      GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, this, 0)
      // GLES20.glTexImage2D(GL10.GL_TEXTURE_2D, 0, GL10.GL_RGBA, width, height, 0, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, ByteBuffer.wrap(RgbBytes))
    }
  }

  private fun applyEffect() { // apply effect from texture[0] to texture[1]
    val image = this.image
    val effectContext = this.effectContext
    if (image != null && effectContext != null) {
      val factory = effectContext.factory
      effect = factory.createEffect(EffectFactory.EFFECT_SEPIA).apply {
        apply(textures[0], image.width, image.height, textures[1])
      }
    }
  }

  // CAMERA

  @Synchronized
  private fun setImage(image: Bitmap) {
    this.image?.recycle()
    this.image = image
  }
  override fun analyze(image: ImageProxy, rotationDegrees: Int) {
    val rotationMatrix = Matrix().apply {
      postRotate(rotationDegrees.toFloat())
    }
    val bitmapRaw = image.image!!.toBitmap()
    val bitmapRotated = Bitmap.createBitmap(bitmapRaw, 0, 0, bitmapRaw.width, bitmapRaw.height, rotationMatrix, true)
    setImage(bitmapRotated)
    gLSurfaceView.requestRender()
  }
}
