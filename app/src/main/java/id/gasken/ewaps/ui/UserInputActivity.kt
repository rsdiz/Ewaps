package id.gasken.ewaps.ui

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import id.gasken.ewaps.R
import id.gasken.ewaps.custom.ImageResizer
import id.gasken.ewaps.custom.SliderAdapter2
import id.gasken.ewaps.custom.SliderItemBitmap
import id.gasken.ewaps.databinding.ActivityUserInputBinding
import id.gasken.ewaps.tool.viewBinding
import java.io.FileNotFoundException
import java.io.IOException
import java.util.* // ktlint-disable no-wildcard-imports
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.abs

class UserInputActivity : AppCompatActivity(), OnMapReadyCallback, OnMarkerDragListener {

    private val binding: ActivityUserInputBinding by viewBinding()

    private lateinit var mMap: GoogleMap

    private val firestore = FirebaseFirestore.getInstance()

    private val mStorageRef = FirebaseStorage.getInstance().reference

    private val reportData: MutableMap<String, Any> = HashMap()

    private val sliderItems: ArrayList<SliderItemBitmap> = ArrayList()

    private var uidString = ""

    private var filePathList = ArrayList<Uri>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mapFragment = (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment)

        mapFragment.getMapAsync(this)

        binding.cameraBtn.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, 0)
        }

        binding.fileBtn.setOnClickListener {
//            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//            startActivityForResult(cameraIntent, 0)

            if (ActivityCompat.checkSelfPermission(
                    this, android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 100
                )

                return@setOnClickListener
            }

            val intent = Intent()
                .setType("image/*")
                .setAction(Intent.ACTION_GET_CONTENT)
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1)
        }

        binding.submitBtn.setOnClickListener {
            if (binding.infoId.text.toString() == "") {
                Toast.makeText(this, "keterangan kosong", Toast.LENGTH_SHORT).show()
            } else {
                addReportData()
            }
        }
    }

    private fun addReportData() {

        val progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Uploading...")
        progressDialog.show()
        uploadImage()

        reportData["description"] = binding.submitBtn.text.toString()

        firestore.collection("report")
            .add(reportData)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "berhasil upload", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "gagal upload", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addImage(bitmap: Bitmap) {
        val sliderItemBitmap = SliderItemBitmap()
        val reduceBitmap = ImageResizer().reduceBitmapSize(bitmap, 480, 640)
        sliderItemBitmap.bitmap = reduceBitmap
        sliderItems.add(sliderItemBitmap)
    }

    private fun showImage() {
        val viewPager2 = binding.viewPagerImageSlider
        viewPager2.adapter = SliderAdapter2(sliderItems, viewPager2)
        viewPager2.clipToPadding = false
        viewPager2.clipChildren = false
        viewPager2.offscreenPageLimit = 3
        viewPager2.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

        val compositePageTransformer = CompositePageTransformer()
        compositePageTransformer.addTransformer(MarginPageTransformer(40))
//
        compositePageTransformer.addTransformer { page, position ->
            val r = 1 - abs(position)
            page.scaleY = 0.85f + r * 0.15f
        }

        viewPager2.setPageTransformer(compositePageTransformer)
    }

    private fun uploadImage() {
        uidString = UUID.randomUUID().toString()
        reportData["imagePath"] = "'images/$uidString'"

        for ((index, value) in filePathList.withIndex()) {
            val ref: StorageReference = mStorageRef.child("images/$uidString/$index")

            ref.putFile(value)
                .addOnSuccessListener {
                }
                .addOnFailureListener {
                }
        }
    }

    override fun onMarkerDragStart(p0: Marker?) {
    }

    override fun onMarkerDrag(p0: Marker?) {
    }

    override fun onMarkerDragEnd(marker: Marker?) {
        if (marker != null) {
            reportData["location"] = marker.position
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)

        val marker = mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney").draggable(true))

        mMap.setMinZoomPreference(15.0F)

        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))

        onMarkerDragEnd(marker)

        mMap.setOnMarkerDragListener(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            0 -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        val bitmap = data.extras?.get("data") as Bitmap
//                        val bitmap: Bitmap =
//                            MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
                        addImage(bitmap)
                    }
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show()
                }
            }

            1 -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        if (data.data != null) {
                            try {
                                val imageUri = data.data!!
                                val bitmap: Bitmap =
                                    MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
                                filePathList.add(imageUri)
                                addImage(bitmap)
                            } catch (e: IOException) {
                                Toast.makeText(this, "Error Occured", Toast.LENGTH_SHORT).show()
                            } catch (e: FileNotFoundException) {
                                Toast.makeText(this, "File Not Found", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val clipData = data.clipData
                            if (clipData != null) {
                                for (i in 0 until clipData.itemCount) {
                                    val imageUri = clipData.getItemAt(i).uri
                                    val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
                                    filePathList.add(imageUri)
                                    addImage(bitmap)
                                }
                            }
                        }
                        showImage()
                    }
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
