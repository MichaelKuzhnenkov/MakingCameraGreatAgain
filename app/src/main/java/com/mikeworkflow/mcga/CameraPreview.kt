package com.mikeworkflow.mcga

import android.content.Context
import android.hardware.Camera
import android.util.DisplayMetrics
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.RelativeLayout

class CameraPreview(context: Context, camera: Camera, private val mainActivity: MainActivity) : SurfaceView(context),
    SurfaceHolder.Callback {
    private var mCamera: Camera = camera
    private var mHolder = holder
    private var mSupportedPreviewSizes = camera.parameters.supportedPreviewSizes

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
            mCamera.parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
            mCamera.parameters.previewSize.width = mainActivity.mPreviewSize!!.width
            mCamera.parameters.previewSize.height = mainActivity.mPreviewSize!!.height
            mCamera.parameters.zoom = 0
            mCamera.parameters.jpegQuality = 100

            mCamera.setPreviewDisplay(mHolder)
            mCamera.setDisplayOrientation(90)
            mCamera.startPreview()

        } catch (e: Exception) {
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {}

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val dm: DisplayMetrics = resources.displayMetrics
        var width = dm.widthPixels.toDouble()
        var height = dm.heightPixels.toDouble()
        val cl: RelativeLayout =
            mainActivity.findViewById(R.id.main_activity_layout) as RelativeLayout
        if (cl.measuredHeight > height)
            height = cl.measuredHeight.toDouble()
        if (cl.measuredWidth > width)
            width = cl.measuredWidth.toDouble()
        if (mSupportedPreviewSizes == null) return

        if (mainActivity.mPreviewSize == null) {
            var optimalSize: Camera.Size? = null
            var deltaH: Double = Double.MAX_VALUE
            var deltaW: Double = Double.MAX_VALUE
            for (size in mSupportedPreviewSizes)
                if (height - size.width < deltaW && width - size.height < deltaH) {
                    optimalSize = size
                    deltaW = height - size.width
                    deltaH = width - size.height
                }
            mainActivity.mPreviewSize = optimalSize!!

            if (deltaH > deltaW) {
                mainActivity.mPreviewSize!!.width = width.toInt()
                mainActivity.mPreviewSize!!.height =
                    (height * width / mainActivity.mPreviewSize!!.height).toInt()
            } else {
                mainActivity.mPreviewSize!!.width =
                    (width * height / mainActivity.mPreviewSize!!.width).toInt()
                mainActivity.mPreviewSize!!.height = height.toInt()
            }


        } else if (height > mainActivity.mPreviewSize!!.height) {
            mainActivity.mPreviewSize!!.height = height.toInt()
            mainActivity.mPreviewSize!!.width =
                (mainActivity.mPreviewSize!!.width * (height / mainActivity.mPreviewSize!!.height)).toInt()
        }
        setMeasuredDimension(
            mainActivity.mPreviewSize!!.width,
            mainActivity.mPreviewSize!!.height
        )

        mainActivity.setupCameraLayout(
            mainActivity.mPreviewSize!!.width,
            mainActivity.mPreviewSize!!.height,
            display
        )
    }
}