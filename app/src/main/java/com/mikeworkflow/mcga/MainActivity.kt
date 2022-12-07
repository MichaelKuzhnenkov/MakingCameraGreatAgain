package com.mikeworkflow.mcga

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.DisplayMetrics
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.mikeworkflow.mcga.databinding.ActivityMainBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

import android.graphics.Matrix
import android.hardware.*
import android.view.*
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity(), PermissionListener, SensorEventListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mCamera: Camera
    private lateinit var mCameraLayout: FrameLayout
    private lateinit var mCameraPreview: CameraPreview
    private lateinit var mShootButton : TextView
    private var mPreviewSize : Camera.Size? = null
    private lateinit var mGalleryThumbnail : ImageView
    override fun onPermissionGranted() {
        initCameraPreview()
    }

    override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        binding = ActivityMainBinding.inflate(layoutInflater)
        mCameraLayout = binding.cameraLayout
        mShootButton = binding.shoot
        mGalleryThumbnail = binding.galleryThumb
        setContentView(binding.root)

        initGalleryThumbnail()
        mShootButton.setOnClickListener { v ->
            mCamera.takePicture(null, null, pictureCallback)
        }

        if (ContextCompat.checkSelfPermission(
                this,
                CAMERA_SERVICE
            ) == PackageManager.PERMISSION_DENIED
        )
            TedPermission.with(this)
                .setPermissionListener(this)
                .setPermissions(Manifest.permission.CAMERA)
                .setPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)
                .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .check()
        else
            initCameraPreview()

        val orientationEventListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {}
        }

        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_NORMAL);
        orientationEventListener.enable()
    }

    override fun onResume() {
        super.onResume()
        initCameraPreview()
    }

    override fun onPause() {
        super.onPause()
        mPreviewSize = null
        mCamera.release()
    }

    private fun initGalleryThumbnail() {
        var bmp: Bitmap
            try {
                thread {
                    bmp = getImageForThumb()
                    runOnUiThread { mGalleryThumbnail.setImageBitmap(bmp) }
                }
            } catch (e : NoSuchFileException) {
                e.printStackTrace()
            }
    }

    fun getImageForThumb() : Bitmap {
        var file = externalMediaDirs.get(0).listFiles()[externalMediaDirs.get(0).listFiles().size-1]
        if (!file.exists())
            throw NoSuchFileException(file)
        var bitmap = BitmapFactory.decodeFile(file.absolutePath)
        //if (bitmap.width>bitmap.height)
        return bitmap
    }

    private fun initCameraPreview() {
        try {
            mCamera = getCameraInstance()!!
            mCameraPreview = CameraPreview(this, mCamera, this)
            mCameraLayout.addView(mCameraPreview)
        } catch (e: Exception) {
        }

    }

    private fun getCameraInstance(): Camera? {
        try {
            return Camera.open()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
    private val pictureCallback = Camera.PictureCallback(function = { bytes: ByteArray, camera: Camera ->
        var bmpImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        if (portrait) {
            val matrix = Matrix()
            matrix.postRotate(90F)
            bmpImage = Bitmap.createBitmap(
                bmpImage, 0, 0, mCameraPreview.width * bmpImage.height / mCameraPreview.height,
                bmpImage.height, matrix, true
            )

        } else
            bmpImage = Bitmap.createBitmap(
                bmpImage, 0, 0, bmpImage.width,
                mCameraPreview.width * bmpImage.width / mCameraPreview.width, null, true
            )


        var outStream = ByteArrayOutputStream()
        bmpImage.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
        var imageFile = File(externalMediaDirs.get(0).absolutePath)
        if (!imageFile.exists())
            imageFile.mkdirs()
        var fo = FileOutputStream(File.createTempFile("mcga_", ".jpeg", imageFile))
        fo.write(outStream.toByteArray())
        fo.close()
        mCamera.startPreview()
        initGalleryThumbnail()
    })


    fun setupCameraLayout(width: Int, height: Int, display: Display) {
        val params = mCameraLayout.layoutParams
        params.height = height
        params.width = width
        mCameraLayout.layoutParams = params
        val btnParams = mShootButton.layoutParams as RelativeLayout.LayoutParams
        when(display.rotation) {
            //rotate interface icons
//            0 -> btnParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
//            1 -> btnParams.addRule(RelativeLayout.ALIGN_PARENT_END)
//            2 -> btnParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
//            3 -> btnParams.addRule(RelativeLayout.ALIGN_PARENT_START)
        }
        mShootButton.layoutParams = btnParams

    }

    class CameraPreview(context: Context, camera: Camera, mainActivity: MainActivity) : SurfaceView(context),
        SurfaceHolder.Callback {
        private var mCamera: Camera = camera
        private var mHolder = holder
        private var mSupportedPreviewSizes = camera.parameters.supportedPreviewSizes
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
                mCamera.parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
                mCamera.parameters.previewSize.width = mainActivity.mPreviewSize!!.width
                mCamera.parameters.previewSize.height = mainActivity.mPreviewSize!!.height
                mCamera.parameters.zoom = 0
                mCamera.parameters.pictureSize.width = mainActivity.mPreviewSize!!.width
                mCamera.parameters.pictureSize.height = mainActivity.mPreviewSize!!.height
                mCamera.parameters.jpegQuality = 100
                mCamera.parameters.jpegThumbnailQuality = 100

//                mCamera.parameters.
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
                mainActivity.mPreviewSize!!.width = (mainActivity.mPreviewSize!!.width * (height/mainActivity.mPreviewSize!!.height)).toInt()
            }
            setMeasuredDimension(
                mainActivity.mPreviewSize!!.width,
                mainActivity.mPreviewSize!!.height)

            mainActivity.setupCameraLayout(
                mainActivity.mPreviewSize!!.width,
                mainActivity.mPreviewSize!!.height,
                display
            )
        }
    }

    var portraitPitch = false
    var landscapePitch = false
    var portraitRoll = false
    var landscapeRoll = false
    var portrait = false
    var landscape = false

    override fun onSensorChanged(event: SensorEvent?) {
        val values = event!!.values

        // Movement
        val azimuth = values[0]
        val pitch = values[1]
        val roll = values[2]

        if (-110 <= pitch && pitch <= -70 || 70 <= pitch && pitch <= 110) {
            //PORTRAIT MODE
            portraitPitch = true
            landscapePitch = false
        } else if (-20 <= pitch && pitch <= 20 || -200 <= pitch && pitch <= -160 || 160 <= pitch && pitch <= 200) {
            //LANDSCAPE MODE
            portraitPitch = false
            landscapePitch = true
        }

        if (-20 <= roll && roll <= 20) {
            //PORTRAIT MODE
            portraitRoll = true
            landscapeRoll = false
        } else if (-110 <= roll && roll <= -70 || 70 <= roll && roll <= 110) {
            //LANDSCAPE MODE
            portraitRoll = false
            landscapeRoll = true
        }

        if (portraitPitch && portraitRoll && !portrait) {
            portrait = true
            landscape = false
            //rotateIconsToPortraitMode()
        }

        if (landscapePitch && landscapeRoll && !landscape) {
            landscape = true
            portrait = false
            //rotateIconsToLandscapeMode()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

}