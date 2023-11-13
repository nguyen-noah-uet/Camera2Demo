package com.example.camera2demo

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private lateinit var captureBtn: Button
    lateinit var cameraManager: CameraManager
    lateinit var textureView: TextureView
    lateinit var cameraCaptureSession: CameraCaptureSession
    lateinit var cameraDevice: CameraDevice
    lateinit var captureRequestBuilder : CaptureRequest.Builder
    lateinit var captureRequest: CaptureRequest
    lateinit var handler: Handler
    lateinit var handlerThread: HandlerThread
    lateinit var imageReader: ImageReader
    val context = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)

            getPermission()


            textureView = findViewById(R.id.textureView)
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

                }
            }


            captureBtn = findViewById<Button>(R.id.captureBtn)
        } catch (e: Exception) {
            e.printStackTrace()
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
//        surfaceTexture?.setDefaultBufferSize(textureView.width, textureView.height)
        val surface = Surface(surfaceTexture)
        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(surface)
        cameraDevice.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                cameraCaptureSession = session
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 6.0f)
                captureRequest = captureRequestBuilder.build()
                cameraCaptureSession.setRepeatingRequest(captureRequest, null, handler)
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show()
            }
        }, handler)
    }

    private fun getPermission() {
        var permissionList = mutableListOf<String>()
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