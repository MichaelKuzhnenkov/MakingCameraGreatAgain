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

    var portrait = false
    var landscape = false
    var mPreviewSize : Camera.Size? = null
    lateinit var binding: ActivityMainBinding
    lateinit var mCamera: Camera
    lateinit var mCameraLayout: FrameLayout
    lateinit var mCameraPreview: CameraPreview
    lateinit var mShootButton : TextView
    lateinit var mGalleryThumbnail : ImageView

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

    fun setupCameraLayout(width: Int, height: Int, display: Display) {
        val params = mCameraLayout.layoutParams
        params.height = height
        params.width = width
        mCameraLayout.layoutParams = params
        //todo: implement buttons rotation when they'll appear in project
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

    private val pictureCallback = Camera.PictureCallback(function = { bytes: ByteArray, camera: Camera ->
        thread {
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
        }
    })

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
        return bitmap
    }

    //thanks to stackoverflown for realisation
    //todo: inspect calculations
    override fun onSensorChanged(event: SensorEvent?) {
        val values = event!!.values
        var portraitPitch = false
        var landscapePitch = false
        var portraitRoll = false
        var landscapeRoll = false
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

    override fun onPermissionGranted() {
        initCameraPreview()
    }

    override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
    }

}