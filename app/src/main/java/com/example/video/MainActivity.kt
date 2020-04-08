package com.example.video

import android.content.res.AssetFileDescriptor
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.example.video.camera.CameraRender
import com.example.video.util.TextureHelper
import java.lang.Exception
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity : AppCompatActivity(),
    View.OnClickListener,
    GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener, MediaPlayer.OnVideoSizeChangedListener {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var videoSeekBar: SeekBar
    private lateinit var playTimeView: Button
    private lateinit var videoThumbnailView: ImageView

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
    private var currentPosition = 0

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
        videoThumbnailView = findViewById(R.id.video_thumbnail)
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
                videoThumbnailView.visibility = View.INVISIBLE
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

        cameraRender?.setProgress(videoGrayConvertProgress)
        Matrix.setIdentityM(mvpMatrix, 0)
        Matrix.scaleM(mvpMatrix, 0, sx, sy, 1f)
        cameraRender?.drawTexture(mvpMatrix, transform, oesTextureId)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: w = $width, h = $height")
        renderWidth = width
        renderHeight = height
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        oesTextureId = TextureHelper.createOESTextureObject()
        surfaceTexture = SurfaceTexture(oesTextureId)
        surfaceTexture!!.setOnFrameAvailableListener(this)

        mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource(assetFileDescriptor)
        mediaPlayer.setOnVideoSizeChangedListener(this)
        val surface = Surface(surfaceTexture)
        mediaPlayer.setSurface(surface)
        surface.release()

        try {
            mediaPlayer.prepareAsync()
        } catch (exp: Exception) {
            exp.printStackTrace()
        }

        cameraRender = CameraRender(this)
        Log.d(TAG, "onSurfaceCreated: ")
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        glSurfaceView.requestRender()
        val position = mediaPlayer.currentPosition
        // 使用prepareAsync，有些视频最后一帧的currentPosition变小了，看起来像是最后一点时间的长度，而不是时间戳
        if (position > currentPosition) {
            currentPosition = position
            videoSeekBar.progress = currentPosition
            playTimeView.text = calculateTime(currentPosition)
        }
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

    override fun onVideoSizeChanged(mp: MediaPlayer?, width: Int, height: Int) {
        videoSeekBar.max = mediaPlayer.duration

        sx = 1f * width / renderWidth
        sy = 1f * height / renderHeight

        Log.d(TAG, "onVideoSizeChanged: w = $width, h = $height, renderWidth = $renderWidth, renderHeight = $renderHeight, sx = $sx, sy = $sy")

        // 首次播放的时候，有些视频video size的宽高有几个像素的变动
        val isPlaying = mediaPlayer.isPlaying
        Log.d(TAG, "onVideoSizeChanged: playing? = $isPlaying")
        if (!isPlaying) {
            val mediaMetadataRetriever = MediaMetadataRetriever()
            mediaMetadataRetriever.setDataSource(
                    assetFileDescriptor.fileDescriptor,
                    assetFileDescriptor.startOffset,
                    assetFileDescriptor.length
            )
            val bitmap = mediaMetadataRetriever.getFrameAtTime(0)
            if (bitmap != null) {
                videoThumbnailView.visibility = View.VISIBLE
                videoThumbnailView.setImageBitmap(bitmap)
                Log.d(TAG, "onVideoSizeChanged: bitmap w = ${bitmap.width}, h = ${bitmap.height}")
            }
            mediaMetadataRetriever.release()
        }
    }
}