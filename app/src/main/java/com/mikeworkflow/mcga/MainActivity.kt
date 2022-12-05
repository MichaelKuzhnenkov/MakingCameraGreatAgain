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
import android.widget.RelativeLayout
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
    private var mPreviewSize : Camera.Size? = null

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

    override fun onDestroy() {
        super.onDestroy()
        mPreviewSize = null
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
//        if (width > height)
        params.height = height
//        else
        params.width = width
        mCameraLayout.layoutParams = params
    }

    class CameraPreview(context: Context, camera: Camera, mainActivity: MainActivity) : SurfaceView(context),
        SurfaceHolder.Callback {
        private var mCamera: Camera = camera
        private var mHolder = holder
        private var mSupportedPreviewSizes = camera.parameters.supportedPreviewSizes
//        private lateinit var mPreviewSize: Camera.Size
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
                mCamera.parameters.previewSize.height = mainActivity.mPreviewSize!!.height
                mCamera.parameters.previewSize.width = mainActivity.mPreviewSize!!.width
                mCamera.parameters.zoom = 0
                mCamera.setPreviewDisplay(mHolder)
                val height = resources.displayMetrics.heightPixels
                val width = resources.displayMetrics.widthPixels
                if (height > width)
                    mCamera.setDisplayOrientation(90)
                mCamera.startPreview()

            } catch (e: Exception) {
            }
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {}

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            if (mainActivity.mPreviewSize == null) {
                val dm: DisplayMetrics = resources.displayMetrics
                var width = dm.widthPixels.toDouble()
                var height = dm.heightPixels.toDouble()
                val cl: RelativeLayout =
                    mainActivity.findViewById(R.id.main_activity_layout) as RelativeLayout
                if (cl.measuredHeight > height)
                    height = cl.measuredHeight.toDouble()
                if (cl.measuredWidth > width)
                    width = cl.measuredWidth.toDouble()
//            val parentH = cl.measuredHeight
//            val parentW = cl.measuredWidth
                if (mSupportedPreviewSizes == null) return

                var optimalSize: Camera.Size? = null
                var deltaH: Double = Double.MAX_VALUE
                var deltaW: Double = Double.MAX_VALUE
                for (size in mSupportedPreviewSizes) {
                    if (height > width) {
                        if (height - size.width < deltaW && width - size.height < deltaH) {
                            optimalSize = size
                            deltaW = height - size.width
                            deltaH = width - size.height
                        }
                    } else
                        if (width - size.width < deltaW && height - size.height < deltaH) {
                            optimalSize = size
                            deltaW = width - size.width
                            deltaH = height - size.height

                        }
                }
                mainActivity.mPreviewSize = optimalSize!!
                if (height > width) {
                    if (deltaH > deltaW) {
                        mainActivity.mPreviewSize!!.width = width.toInt()
                        mainActivity.mPreviewSize!!.height =
                            (height * width / mainActivity.mPreviewSize!!.height).toInt()
                    } else {
                        mainActivity.mPreviewSize!!.width =
                            (width * height / mainActivity.mPreviewSize!!.width).toInt()
                        mainActivity.mPreviewSize!!.height = height.toInt()
                    }
                    setMeasuredDimension(
                        mainActivity.mPreviewSize!!.width,
                        mainActivity.mPreviewSize!!.height
                    )
                } else {
                    if (deltaH > deltaW) {
                        mainActivity.mPreviewSize!!.height = height.toInt()
                        mainActivity.mPreviewSize!!.width =
                            (width * height / mainActivity.mPreviewSize!!.height).toInt()
                    } else {
                        mainActivity.mPreviewSize!!.height =
                            (height * width / mainActivity.mPreviewSize!!.width).toInt()
                        mainActivity.mPreviewSize!!.width = width.toInt()
                    }
                    setMeasuredDimension(
                        mainActivity.mPreviewSize!!.width,
                        mainActivity.mPreviewSize!!.height
                    )
                }

                mainActivity.setupCameraLayout(
                    mainActivity.mPreviewSize!!.width,
                    mainActivity.mPreviewSize!!.height
                )
            } else
                setMeasuredDimension(
                    mainActivity.mPreviewSize!!.width,
                    mainActivity.mPreviewSize!!.height
                )
        }
    }
}