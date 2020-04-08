package com.example.video

import android.content.res.AssetFileDescriptor
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
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
    private lateinit var playTimeView: Button

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var assetFileDescriptor: AssetFileDescriptor

    private var surfaceTexture: SurfaceTexture? = null
    private var cameraRender: CameraRender? = null
    private var transform = FloatArray(16)
    private var mvpMatrix = FloatArray(16)
    private var oesTextureId = -1

    private var renderWidth = -1
    private var renderHeight = -1

    private var sx = 1f
    private var sy = 1f

    private var videoGrayConvertProgress = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.play).setOnClickListener(this)
        findViewById<Button>(R.id.stop).setOnClickListener(this)
        findViewById<Button>(R.id.convert).setOnClickListener(this)

        assetFileDescriptor = assets.openFd("demo.mp4")
        glSurfaceView = findViewById(R.id.surface_view)
        glSurfaceView.setEGLContextClientVersion(2)
        glSurfaceView.setRenderer(this)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

        videoSeekBar = findViewById(R.id.video_seek_bar)
        playTimeView = findViewById(R.id.play_time)
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
                if (!mediaPlayer.isPlaying) {
                    mediaPlayer.start()
                }
            }
            R.id.stop -> {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.pause()
                }
            }
            R.id.convert -> {
                videoGrayConvertProgress = if (videoGrayConvertProgress == 0f) 1f else 0f
            }
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        surfaceTexture?.updateTexImage()
        surfaceTexture?.getTransformMatrix(transform)

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClearColor(255f, 255f, 255f, 1f)
        GLES20.glViewport(0, 0, renderWidth, renderHeight)
        cameraRender?.setProgress(videoGrayConvertProgress)
        Matrix.setIdentityM(mvpMatrix, 0)
        Matrix.scaleM(mvpMatrix, 0, sx, sy, 1f)
        cameraRender?.drawTexture(mvpMatrix, transform, oesTextureId)
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
            val display = windowManager.defaultDisplay
            val point = Point()
            display.getSize(point)
            renderWidth = point.x
            renderHeight = point.y

            sx = 1f * width / renderWidth
            sy = 1f * height / renderHeight

            Log.d(TAG, "video size: w = $width, h = $height, renderWidth = $renderWidth, renderHeight = $renderHeight, sx = $sx, sy = $sy")

            // before start, we draw video frame once.
            glSurfaceView.requestRender()
        }

        videoSeekBar.max = mediaPlayer.duration
        cameraRender = CameraRender(this)
        Log.d(TAG, "onSurfaceCreated: ")
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        glSurfaceView.requestRender()
        val currentPosition = mediaPlayer.currentPosition
        videoSeekBar.progress = currentPosition
        playTimeView.text = calculateTime(currentPosition)
    }

    private fun calculateTime(timeMs: Int): String {
        val time = timeMs / 1000
        val hour = time / 3600
        val minute = time / 60
        val second = time - hour * 3600 - minute * 60
        return "${alignment(hour)}:${alignment(minute)}:${alignment(second)}"
    }

    private fun alignment(time: Int): String {
        return if (time > 9) "$time" else "0$time"
    }
}