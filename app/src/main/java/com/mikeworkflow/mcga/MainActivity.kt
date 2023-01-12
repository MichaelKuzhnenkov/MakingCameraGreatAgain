package com.mikeworkflow.mcga

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.mikeworkflow.mcga.databinding.ActivityMainBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

import android.hardware.*
import android.hardware.Camera
import android.view.*
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity(), PermissionListener, SensorEventListener {

    var portrait = false
    var landscape = false
    var rotation = 0
    lateinit var binding: ActivityMainBinding
    var mCamera: Camera? = null
    lateinit var mCameraLayout: FrameLayout
    lateinit var mCameraPreview: CameraPreview
    lateinit var mShootButton : TextView
    lateinit var mGalleryThumbnail : ImageView
    lateinit var mChangeRatioList : ExpandableListView
    lateinit var ratioListAdapter : CustomExpandableListAdapter
    var selectedRatio : String = RatioValues.WIDESCREEN.ratio

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        binding = ActivityMainBinding.inflate(layoutInflater)
        mCameraLayout = binding.cameraLayout
        mShootButton = binding.shoot
        mGalleryThumbnail = binding.galleryThumb
        mChangeRatioList = binding.changeRatioList
        setContentView(binding.root)

        mShootButton.setOnClickListener { v ->
            mCamera!!.takePicture(null, null, pictureCallback)
        }
        initRatioAdapter(RatioValues.WIDESCREEN.ratio)
        mChangeRatioList.setOnChildClickListener(expandableListChildClickListener)

        checkPermissionsGranted()
        initGalleryThumbnail()

        val orientationEventListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {}
        }

        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_NORMAL)
        orientationEventListener.enable()
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
        initCameraPreview()
    }

    override fun onPause() {
        super.onPause()
        if (mCamera != null)
            mCamera!!.release()
    }

    private fun initCameraPreview() {
        try {
            mCamera = getCameraInstance()
            if (mCamera != null) {
                mCameraPreview = CameraPreview(this, mCamera!!, this)
                mCameraLayout.addView(mCameraPreview)
            }
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

    fun setupCameraLayout(width: Int, height: Int) {

//        var params = mCameraLayout.layoutParams
//        params.height = height
//        params.width = width
        //mCameraLayout.layoutParams = params
    }

    private val pictureCallback = Camera.PictureCallback(function = { bytes: ByteArray, camera: Camera ->
        thread {
            var bmpImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (portrait) {
                val matrix = Matrix()
                matrix.postRotate(90F)

                bmpImage = Bitmap.createBitmap(
                    bmpImage, 0, 0, bmpImage.width,
                    bmpImage.height, matrix, true
                )
            } else
//                bmpImage = Bitmap.createBitmap(
//                    bmpImage, 0, bmpImage.height - (mCameraPreview.width * bmpImage.width / mCameraPreview.height), bmpImage.width,
//                    mCameraPreview.width * bmpImage.width / mCameraPreview.height, null, true
//                )
                bmpImage = Bitmap.createBitmap(
                    bmpImage, 0, 0, bmpImage.width,
                    bmpImage.height, null, true
                )

            val outStream = ByteArrayOutputStream()
            bmpImage.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
            val imageFile = File(externalMediaDirs.get(0).absolutePath)
            if (!imageFile.exists())
                imageFile.mkdirs()
            val fo = FileOutputStream(File.createTempFile("mcga_", ".jpeg", imageFile))
            fo.write(outStream.toByteArray())
            fo.close()
            initGalleryThumbnail()
        }
        mCamera!!.startPreview()
    })

    private fun initGalleryThumbnail() {
        var bmp: Bitmap

            thread {
                try {
                //getting last made photo and make a round image from center of it
                bmp = getImageForThumb()
                //crop an image to square
                if (bmp.height > bmp.width)
                    bmp = Bitmap.createBitmap(
                        bmp, 0,
                        (bmp.height - bmp.width) / 2, bmp.width, bmp.width,
                        null, true
                    )
                else
                    bmp = Bitmap.createBitmap(
                        bmp, (bmp.width - bmp.height) / 2,
                        0, bmp.height, bmp.height,
                        null, true
                    )
                //rounding it
                val roundIcon = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(roundIcon)
                val paint = Paint()
                paint.isAntiAlias = true
                val rect = Rect(0, 0, bmp.width, bmp.height)
                canvas.drawCircle(
                    bmp.width / 2.toFloat(),
                    bmp.height / 2.toFloat(), bmp.width / 2.toFloat(), paint
                )
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                canvas.drawBitmap(bmp, rect, rect, paint)

                runOnUiThread { mGalleryThumbnail.setImageBitmap(roundIcon) }
                } catch (e : Exception) {
                    e.printStackTrace()
                }
            }

    }

    fun getImageForThumb(): Bitmap {
        try {
            val file : File
            if (externalMediaDirs.isNotEmpty() && externalMediaDirs.get(0).listFiles().isNotEmpty()) {
                file =
                    externalMediaDirs.get(0).listFiles()!![externalMediaDirs.get(0)
                        .listFiles()!!.size - 1]
                if (!file.exists())
                    throw NoSuchFileException(file)
                return BitmapFactory.decodeFile(file.absolutePath)
            }
            else throw SecurityException()
        } catch (e : Exception) {
            throw Exception(e)
        }
    }

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
        }

        if (landscapePitch && landscapeRoll && !landscape) {
            landscape = true
            portrait = false
        }

        //on fast device 180 degree rotation there won't be switching between modes,
        //so upper statements won't update the rotation state when in changed
        //in that case we need more checks
        if (portraitPitch && portraitRoll && portrait)
            if (pitch < 0) {
                rotateButtons(rotation, 0)
                rotation = 0
            }
            else {
                rotateButtons(rotation, 2)
                rotation = 2
            }

        if (landscapePitch && landscapeRoll && landscape)
            if (roll > 0) {
                rotateButtons(rotation, 1)
                rotation = 1
            }
            else {
                rotateButtons(rotation, 3)
                rotation = 3
            }
    }

    private fun rotateButtons(previous : Int, current : Int) {
        if (previous == current) return

        if (previous == 3 && current == 0)
            mGalleryThumbnail.animate().rotation(360F).withEndAction { mGalleryThumbnail.rotation = 0F }.start()
        else if (previous == 0 && current == 3)
            mGalleryThumbnail.animate().rotation(-90F).withEndAction { mGalleryThumbnail.rotation = 270F }.start()
        else
            mGalleryThumbnail.animate().rotation(current*90F).start()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    private fun checkPermissionsGranted() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED)
            TedPermission.with(this)
                .setPermissionListener(this)
                .setPermissions(Manifest.permission.CAMERA)
                .check()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
            TedPermission.with(this)
                .setPermissionListener(this)
                .setPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)
                .check()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
            TedPermission.with(this)
                .setPermissionListener(this)
                .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .check()
    }

    override fun onPermissionGranted() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
            initCameraPreview()
    }

    override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
    }

    fun initRatioAdapter(header : String) {
        val ratioTitle : ArrayList<String> = ArrayList()
        ratioTitle.add(header)
        val ratioMap : HashMap<String, ArrayList<String>> = HashMap()
        val ratioList : ArrayList<String> = ArrayList()
        ratioList.addAll(arrayListOf(RatioValues.WIDESCREEN.ratio, RatioValues.R_16_9.ratio, RatioValues.R_4_3.ratio))
        ratioList.remove(header)
        ratioMap.put(ratioTitle.get(0), ratioList)

        ratioListAdapter = CustomExpandableListAdapter(this, ratioTitle, ratioMap)
        mChangeRatioList.setAdapter(ratioListAdapter)
        mChangeRatioList.deferNotifyDataSetChanged()
    }

    val expandableListChildClickListener = ExpandableListView.OnChildClickListener { parent, v, groupPosition, childPosition, id ->
        val groupName = ratioListAdapter.getGroup(groupPosition) as String
        val childName = ratioListAdapter.getChild(groupPosition, childPosition) as String
        if (!childName.equals(groupName)) {
            initRatioAdapter(childName)
            selectedRatio = childName

            if (mCamera != null)
                mCamera!!.stopPreview()
            when(selectedRatio) {
                RatioValues.WIDESCREEN.ratio -> setupCameraLayout(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
                RatioValues.R_16_9.ratio -> setupCameraLayout(resources.displayMetrics.widthPixels, (resources.displayMetrics.widthPixels * 1.7777).toInt())
                RatioValues.R_4_3.ratio -> setupCameraLayout(resources.displayMetrics.widthPixels, (resources.displayMetrics.widthPixels * 1.3333).toInt())
            }

            mCameraPreview.mPreviewSize = null
            if (mCamera!=null)
                mCamera!!.startPreview()
        }
        return@OnChildClickListener true
    }

}