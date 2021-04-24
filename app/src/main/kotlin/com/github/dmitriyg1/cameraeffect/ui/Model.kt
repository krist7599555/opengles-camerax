package com.github.dmitriyg1.cameraeffect.ui

import android.opengl.GLES20
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.*

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

class Model(
  val positions: FloatArray,
  val normals: FloatArray,
  val textures: FloatArray
) {

  val numFaces = positions.size / 3
  val positionsBuffer = floatarrayToBuffer(positions)
  val normalsBuffer = floatarrayToBuffer(normals)
  val texturesBuffer = floatarrayToBuffer(textures)

  init {
    assert(positions.size == normals.size, { "length should be equal" })
    assert(positions.size == textures.size, { "length should be equal" })
  }

  fun bindPositionAttributeTo(handle: Int) =
    GLES20.glVertexAttribPointer(handle, 3, GLES20.GL_FLOAT, false, 0, positionsBuffer)

  fun bindNormalAttributeTo(handle: Int) =
    GLES20.glVertexAttribPointer(handle, 3, GLES20.GL_FLOAT, false, 0, normalsBuffer)

  fun bindTextureAttributeTo(handle: Int) =
    GLES20.glVertexAttribPointer(handle, 2, GLES20.GL_FLOAT, false, 0, texturesBuffer)

  fun draw() {
    Log.d("Model", "numFaces = $numFaces")
    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, numFaces)
  }

  companion object {
    // https://docs.safe.com/fme/2017.0/html/FME_Desktop_Documentation/FME_ReadersWriters/obj/Supported_OBJ_File_Synta.htm
    fun fromOBJ(obj: String): Model {
      Log.d("Model.OBJParser", "parse this:\n$obj")
      val verticesOBJ = Vector<Float>();
      val normalsOBJ = Vector<Float>();
      val texturesOBJ = Vector<Float>();
      val facesOBJ = Vector<String>();
      obj
        .lines()
        .forEach { _line ->
          val line = _line.trim().split(' ')
          val cmd = line.first()
          val args = line.subList(1, line.size)
          when (cmd) {
            "", "#" -> {
            }
            "mtllib", "usemtl", "g", "s", "o" -> {
              Log.w("Model.OBJParser", "TODO implement OBJ parser $cmd")
            }
            "v" -> args.forEach { verticesOBJ.add(it.toFloat()) }
            "vn" -> args.forEach { normalsOBJ.add(it.toFloat()) }
            "vt" -> args.forEach { texturesOBJ.add(it.toFloat()) }
            "f" -> {
              when (args.size) {
                3 -> facesOBJ.apply {
                  add(args[0])
                  add(args[1])
                  add(args[2])
                }
                4 -> facesOBJ.apply {
                  add(args[0])
                  add(args[1])
                  add(args[2])
                  add(args[2])
                  add(args[3])
                  add(args[0])
                }
              }
            }
            else -> throw Exception("OBJ parser not recognize $_line")
          }
        }

      val positions = FloatArray(facesOBJ.size * 3);
      val normals = FloatArray(facesOBJ.size * 3);
      val textures = FloatArray(facesOBJ.size * 2);

      var pi = 0;
      var ni = 0;
      var ti = 0;
      for (face in facesOBJ) {
        val parts = face.split("/").map { it.toInt() - 1 };

        var index = 0

        index = 3 * parts[0];
        positions[pi++] = verticesOBJ[index++];
        positions[pi++] = verticesOBJ[index++];
        positions[pi++] = verticesOBJ[index++];

        index = 2 * parts[1];
        textures[ni++] = texturesOBJ[index++];
        textures[ni++] = 1 - texturesOBJ[index++];         // NOTE: Bitmap gets y-invert]d

        index = 3 * parts[2];
        normals[ti++] = normalsOBJ[index++];
        normals[ti++] = normalsOBJ[index++];
        normals[ti++] = normalsOBJ[index++];
      }
      assert(pi == positions.size, { "positions should use all" })
      assert(ni == normals.size, { "normals should use all" })
      assert(ti == textures.size, { "textures should use all" })

      return Model(positions, normals, textures)
    }
  }
}