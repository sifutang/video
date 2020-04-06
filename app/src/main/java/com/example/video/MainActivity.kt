package com.example.video

import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import java.lang.Exception

class MainActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var surfaceView: SurfaceView
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var assetFileDescriptor: AssetFileDescriptor

    private var holderCallback = object : SurfaceHolder.Callback {
        override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            Log.d(TAG, "surfaceChanged: w = $width, h = $height, format = $format")
        }

        override fun surfaceDestroyed(holder: SurfaceHolder?) {
            Log.d(TAG, "surfaceDestroyed: ")
        }

        override fun surfaceCreated(holder: SurfaceHolder?) {
            Log.d(TAG, "surfaceCreated: ")
            mediaPlayer.setDisplay(holder)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.play).setOnClickListener(this)

        assetFileDescriptor = assets.openFd("demo2.mp4")
        surfaceView = findViewById(R.id.surface_view)
        surfaceView.holder.addCallback(holderCallback)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: ")
        mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource(assetFileDescriptor)

        try {
            mediaPlayer.prepare()
        } catch (exp: Exception) {
            exp.printStackTrace()
        }

        mediaPlayer.setOnVideoSizeChangedListener { mp, width, height ->
            val layoutParams = surfaceView.layoutParams
            val viewWidth = layoutParams.width
            val viewHeight = layoutParams.height
            if (viewWidth != width || viewHeight != height) {
                layoutParams.width = width
                layoutParams.height = height
                surfaceView.layoutParams = layoutParams
            }
            Log.d(TAG, "onResume: w = $width, h = $height, viewWidth = $viewWidth, viewHeight = $viewHeight")
        }
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
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.play -> {
                mediaPlayer.start()
            }
        }
    }
}