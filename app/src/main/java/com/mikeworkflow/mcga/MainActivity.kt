package com.mikeworkflow.mcga

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.mikeworkflow.mcga.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), PermissionListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mCamera: Camera
    private lateinit var mCameraLayout: FrameLayout
    private lateinit var mCameraPreview: CameraPreview

    override fun onPermissionGranted() {
        initCameraPreview()
    }

    override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        mCameraLayout = binding.cameraLayout
        setContentView(binding.root)

        if (ContextCompat.checkSelfPermission(
                this,
                CAMERA_SERVICE
            ) == PackageManager.PERMISSION_DENIED
        )
            TedPermission.with(this)
                .setPermissionListener(this)
                .setPermissions(Manifest.permission.CAMERA)
                .check()
        else
            initCameraPreview()
    }

    private fun initCameraPreview() {
        try {
            mCamera = getCameraInstance()
        } catch (e: Exception) {
        }

        mCameraPreview = CameraPreview(this, mCamera, this)
        mCameraLayout.addView(mCameraPreview)
    }

    private fun getCameraInstance(): Camera {
        try {
            return Camera.open()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Camera.open()
    }

    public fun setupCameraLayout(width: Int, height: Int) {
        var params = mCameraLayout.layoutParams
        params.height = height
        params.width = width
        mCameraLayout.layoutParams = params
    }

    class CameraPreview(context: Context, camera: Camera, mainActivity: MainActivity) : SurfaceView(context),
        SurfaceHolder.Callback {
        private var mCamera: Camera = camera
        private var mHolder = holder
        private var mSupportedPreviewSizes = camera.parameters.supportedPreviewSizes
        private lateinit var mPreviewSize: Camera.Size
        private val mainActivity = mainActivity

        init {
            mHolder.addCallback(this)
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        }


        override fun surfaceCreated(holder: SurfaceHolder) {
            try {
                mCamera.setPreviewDisplay(holder)
                mCamera.startPreview()
            } catch (e: Exception) {
            }
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            if (mHolder.surface == null)
                return
            try {
                mCamera.stopPreview()
            } catch (e: Exception) {
            }

            try {
                mCamera.setPreviewCallback(object : Camera.PreviewCallback {
                    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {

                    }
                })
                mCamera.parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
                mCamera.parameters.previewSize.height = mPreviewSize.height
                mCamera.parameters.previewSize.width = mPreviewSize.width
                mCamera.parameters.zoom = 0
                mCamera.setPreviewDisplay(mHolder)
                mainActivity.setupCameraLayout(mPreviewSize.width, mPreviewSize.height)
                mCamera.startPreview()

            } catch (e: Exception) {
            }
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {}

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val dm : DisplayMetrics = resources.displayMetrics
            val width = dm.widthPixels.toDouble()
            val height = dm.heightPixels.toDouble()

            if (mSupportedPreviewSizes == null) return

            var optimalSize: Camera.Size? = null
            var deltaH : Double = Double.MAX_VALUE
            var deltaW: Double = Double.MAX_VALUE
            for (size in mSupportedPreviewSizes) {
                if (width - size.width < deltaW && height - size.height < deltaH) {
                    optimalSize = size
                    deltaW = width - size.width
                    deltaH = height - size.height

                }
            }
            mPreviewSize = optimalSize!!
            if (deltaH>deltaW) {
                mPreviewSize.height = height.toInt()
                mPreviewSize.width = (width * height/mPreviewSize.height).toInt()
            } else {
                mPreviewSize.height = (height * width / mPreviewSize.width).toInt()
                mPreviewSize.width = width.toInt()
            }

            setMeasuredDimension(mPreviewSize.width, mPreviewSize.height)

        }
    }
}