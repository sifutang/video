package com.example.video

import android.content.res.AssetFileDescriptor
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.example.video.camera.CameraRender
import com.example.video.util.TextureHelper
import java.lang.Exception
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity : AppCompatActivity(),
    View.OnClickListener,
    GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener{

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var videoSeekBar: SeekBar
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var assetFileDescriptor: AssetFileDescriptor

    private var surfaceTexture: SurfaceTexture? = null
    private var cameraRender: CameraRender? = null
    private var transform = FloatArray(16)
    private var oesTextureId = -1

    private var videoWith = -1
    private var videoHeight = -1

    private var renderWidth = -1
    private var renderHeight = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.play).setOnClickListener(this)

        assetFileDescriptor = assets.openFd("demo.mp4")
        glSurfaceView = findViewById(R.id.surface_view)
        glSurfaceView.setEGLContextClientVersion(2)
        glSurfaceView.setRenderer(this)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

        videoSeekBar = findViewById(R.id.video_seek_bar)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: ")
        glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: ")
        try {
            mediaPlayer.stop()
        } catch (exp: IllegalStateException) {
            exp.printStackTrace()
        }

        mediaPlayer.release()
        surfaceTexture?.release()
        glSurfaceView.onPause()
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.play -> {
                mediaPlayer.start()
            }
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        surfaceTexture?.updateTexImage()
        surfaceTexture?.getTransformMatrix(transform)

        GLES20.glViewport(0, 0, renderWidth, renderHeight)
        cameraRender?.setProgress(0.0f)
        cameraRender?.drawTexture(transform, oesTextureId)

        GLES20.glViewport(0, renderHeight, renderWidth, renderHeight)
        cameraRender?.setProgress(1.0f)
        cameraRender?.drawTexture(transform, oesTextureId)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: w = $width, h = $height")
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        oesTextureId = TextureHelper.createOESTextureObject()
        surfaceTexture = SurfaceTexture(oesTextureId)
        surfaceTexture!!.setOnFrameAvailableListener(this)

        mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource(assetFileDescriptor)
        val surface = Surface(surfaceTexture)
        mediaPlayer.setSurface(surface)
        surface.release()

        try {
            mediaPlayer.prepare()
        } catch (exp: Exception) {
            exp.printStackTrace()
        }

        mediaPlayer.setOnVideoSizeChangedListener { mp, width, height ->
            videoWith = width
            videoHeight = height

            val display = windowManager.defaultDisplay
            val point = Point()
            display.getSize(point)
            renderWidth = point.x
            renderHeight = (renderWidth / (1.0f * videoWith / videoHeight)).toInt()

            Log.d(TAG, "setOnVideoSizeChangedListener: w = $width, h = $height, renderWidth = $renderWidth, renderHeight = $renderHeight")
        }

        videoSeekBar.max = mediaPlayer.duration
        cameraRender = CameraRender(this)
        Log.d(TAG, "onSurfaceCreated: ")
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        glSurfaceView.requestRender()
        videoSeekBar.progress = mediaPlayer.currentPosition
    }
}