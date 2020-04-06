package com.example.video.camera

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.example.video.R
import com.example.video.util.ShaderProgram

class CameraShaderProgram(context: Context) :
        ShaderProgram(context, R.raw.vertex_camera, R.raw.fragment_camera) {

    // Uniform locations
    private var uTextureMatrixLocation = -1
    private var uTextureSamplerLocation = -1
    private var uIdentityLocation = -1

    // Attribute locations
    private var aPositionLocation = -1
    private var aTextureCoordinateLocation = -1

    private var identity = 0f

    init {
        // Retrieve attribute locations for the shader program.
        aPositionLocation = GLES20.glGetAttribLocation(programId, "a_Position")
        aTextureCoordinateLocation = GLES20.glGetAttribLocation(programId, "a_TextureCoordinate")

        // Retrieve uniform locations for the shader program
        uTextureMatrixLocation = GLES20.glGetUniformLocation(programId, "u_TextureMatrix")
        uTextureSamplerLocation = GLES20.glGetUniformLocation(programId, "u_TextureSampler")
        uIdentityLocation = GLES20.glGetUniformLocation(programId, "identity")
    }

    fun setProgress(progress: Float) {
        identity = progress
    }

    fun setUniform(matrix: FloatArray, textureId: Int) {
        GLES20.glUniformMatrix4fv(uTextureMatrixLocation,
                1, false, matrix, 0)
        GLES20.glUniform1f(uIdentityLocation, identity)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glUniform1i(uTextureSamplerLocation, 0)
    }

    fun getPositionAttributeLoc(): Int {
        return aPositionLocation
    }

    fun getTextureCoordinateAttributeLoc(): Int {
        return aTextureCoordinateLocation
    }
}