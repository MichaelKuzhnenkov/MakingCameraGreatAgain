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
    var mPreviewSize : Camera.Size? = null

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
            when (mainActivity.selectedRatio) {
                (RatioValues.WIDESCREEN.ratio) -> if (mPreviewSize!!.height/mPreviewSize!!.width > 1.9) {
                    mCamera.parameters.previewSize.width = mPreviewSize!!.height
                    mCamera.parameters.previewSize.height = mPreviewSize!!.width
                }
                (RatioValues.R_16_9.ratio) -> if (mPreviewSize!!.height/mPreviewSize!!.width > 1.7) {
                    mCamera.parameters.previewSize.width = mPreviewSize!!.height
                    mCamera.parameters.previewSize.height = mPreviewSize!!.width
                }
                (RatioValues.R_4_3.ratio) -> if (mPreviewSize!!.height/mPreviewSize!!.width > 1.3) {
                    mCamera.parameters.previewSize.width = mPreviewSize!!.height
                    mCamera.parameters.previewSize.height = mPreviewSize!!.width
                }
            }
            mCamera.parameters.zoom = 0
            mCamera.parameters.jpegQuality = 100
            mCamera.setDisplayOrientation(90)
            mCamera.setPreviewDisplay(mHolder)

            mCamera.startPreview()

        } catch (e: Exception) {
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {}

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width : Double
        var height : Double
        val dm: DisplayMetrics = resources.displayMetrics
        width = dm.widthPixels.toDouble()
        height = dm.heightPixels.toDouble()

        when(mainActivity.selectedRatio) {
            (RatioValues.WIDESCREEN.ratio) -> {
                val cl: RelativeLayout =
                    mainActivity.findViewById(R.id.main_activity_layout) as RelativeLayout
                if (cl.measuredHeight > height)
                    height = cl.measuredHeight.toDouble()
                if (cl.measuredWidth > width)
                    width = cl.measuredWidth.toDouble()
            }
            (RatioValues.R_16_9.ratio) -> height = width * 1.777
            (RatioValues.R_4_3.ratio) -> height = width * 1.333
        }

        if (mSupportedPreviewSizes == null) return
//      temporary switch to 3:4 ratio only
//        if (mPreviewSize == null) {
//            var optimalSize: Camera.Size? = null
//            var deltaH: Double = Double.MAX_VALUE
//            var deltaW: Double = Double.MAX_VALUE
//            val targetRatio = height/width
//            val ratioDiv = 0.01
//            for (size in mSupportedPreviewSizes)
////                if (height - size.width < deltaW && width - size.height < deltaH) {
////                    optimalSize = size
////                    deltaW = height - size.width
////                    deltaH = width - size.height
////                }
//            if (size.width/size.height - targetRatio < ratioDiv) {
//                if (optimalSize == null)
//                    optimalSize = size
//                if (height - size.width < deltaW && width - size.height < deltaH) {
//                    optimalSize = size
//                    deltaW = height - size.width
//                    deltaH = width - size.height
//                }
//            }
//
//            mPreviewSize = optimalSize!!
//
//            if (deltaH > deltaW) {
//                mPreviewSize!!.width = width.toInt()
//                mPreviewSize!!.height =
//                    (height * width / mPreviewSize!!.height).toInt()
//            } else {
//                mPreviewSize!!.width =
//                    (width * height / mPreviewSize!!.width).toInt()
//                mPreviewSize!!.height = height.toInt()
//            }
//
//        } else if (height/width > 1.9 && height > mPreviewSize!!.height) {
//            mPreviewSize!!.height = height.toInt()
//            mPreviewSize!!.width =
//                (mPreviewSize!!.width * (height / mPreviewSize!!.height)).toInt()
//        }
        mPreviewSize = mSupportedPreviewSizes[0]
        mPreviewSize!!.width = width.toInt()
        mPreviewSize!!.height = (width * 1.34).toInt()
        setMeasuredDimension(
            mPreviewSize!!.width,
            mPreviewSize!!.height
        )
    }
}