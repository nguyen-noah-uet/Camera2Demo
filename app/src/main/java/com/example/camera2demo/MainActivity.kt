package com.example.camera2demo

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import com.google.android.material.slider.Slider

class MainActivity : AppCompatActivity() {
    private val TAG = "CAMERA2DEMO_MainActivity"
    private var isAF = false
    private var isAWB = false
    private lateinit var captureBtn: Button
    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private lateinit var cameraCaptureSession: CameraCaptureSession
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureRequestBuilder : CaptureRequest.Builder
    private lateinit var captureRequest: CaptureRequest
    private lateinit var handler: Handler
    private lateinit var handlerThread: HandlerThread
    private lateinit var imageReader: ImageReader
    private lateinit var focusDistanceSlider: Slider
    private lateinit var autoFocusModeSwitch: SwitchCompat
    private lateinit var autoWBMode: SwitchCompat
    private lateinit var bitmap: Bitmap

    val context = this


    private fun setFocusDistance(distance: Float) {
        captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, distance)
        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
    }
    private fun getFocusDistance(): Float? {
        return captureRequestBuilder.get(CaptureRequest.LENS_FOCUS_DISTANCE)
    }
    private fun setFocusMode(mode: Int) {
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, mode)
        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)
            bindControls()
            wireEvents()
            getPermission()
            cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

            handlerThread = HandlerThread("Camera2")
            handlerThread.start()
            handler = Handler(handlerThread.looper)

            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                    open_camera()
                }

                override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {

                }

                override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                    return false
                }

                override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
                    bitmap = textureView.bitmap!!
                    var focusDistance = getFocusDistance()!!
                    Log.d(TAG, "focus distance: $focusDistance")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun bindControls() {
        textureView = findViewById(R.id.textureView)
        captureBtn = findViewById<Button>(R.id.captureBtn)
        focusDistanceSlider = findViewById<Slider>(R.id.focusDistanceSlider)
        autoFocusModeSwitch = findViewById<SwitchCompat>(R.id.autoFocusModeSwitch)
        autoWBMode = findViewById<SwitchCompat>(R.id.autoWBMode)
    }
    @SuppressLint("ClickableViewAccessibility")
    private fun wireEvents() {

        autoFocusModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            isAF = isChecked
            if (isAF) {
                setFocusMode(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)

                // disable focus distance slider
                focusDistanceSlider.isEnabled = false
            }
            else {
                setFocusMode(CaptureRequest.CONTROL_AF_MODE_OFF)
                // enable focus distance slider
                focusDistanceSlider.isEnabled = true
            }
        }

        focusDistanceSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
            }

            override fun onStopTrackingTouch(slider: Slider) {
                if(!isAF)
                    setFocusDistance(slider.value)
            }
        })

        textureView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                try {
                    // handleTouchToFocus(event.x, event.y)
                    return@setOnTouchListener true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return@setOnTouchListener false
        }
    }


    private fun open_camera() {
        if (ActivityCompat.checkSelfPermission(this, permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        cameraManager.openCamera(cameraManager.cameraIdList[0], object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                takePreview()
            }

            override fun onDisconnected(camera: CameraDevice) {
                cameraDevice.close()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                cameraDevice.close()
            }
        }, handler)
    }

    private fun takePreview() {
        val surfaceTexture = textureView.surfaceTexture
        val surface = Surface(surfaceTexture)
        surfaceTexture?.setDefaultBufferSize(textureView.width, textureView.height)

        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(surface)
        cameraDevice.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                cameraCaptureSession = session
                // turn off autofocus
                setFocusMode(CaptureRequest.CONTROL_AF_MODE_OFF)

                captureRequest = captureRequestBuilder.build()
                cameraCaptureSession.setRepeatingRequest(captureRequest, null, null)
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show()
            }
        }, handler)

    }

    private fun getPermission() {
        val permissionList = mutableListOf<String>()
        if (checkSelfPermission(permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(permission.CAMERA)
        }
        if (permissionList.size > 0){
            requestPermissions(permissionList.toTypedArray(), 101)
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        grantResults.forEach {
            if (it != PackageManager.PERMISSION_GRANTED) {
                finish()
            }
        }
    }
}