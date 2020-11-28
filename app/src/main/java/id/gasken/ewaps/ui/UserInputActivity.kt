package id.gasken.ewaps.ui

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import id.gasken.ewaps.R
import id.gasken.ewaps.custom.ImageResizer
import id.gasken.ewaps.custom.SliderAdapter2
import id.gasken.ewaps.custom.SliderItemBitmap
import id.gasken.ewaps.databinding.ActivityUserInputBinding
import id.gasken.ewaps.tool.Const
import id.gasken.ewaps.tool.viewBinding
import maes.tech.intentanim.CustomIntent
import java.io.FileNotFoundException
import java.io.IOException
import java.util.* // ktlint-disable no-wildcard-imports
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.abs

class UserInputActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val binding: ActivityUserInputBinding by viewBinding()

    private val firestore = FirebaseFirestore.getInstance()

    private val mStorageRef = FirebaseStorage.getInstance().reference

    private val reportData: MutableMap<String, Any> = HashMap()

    private val sliderItems: ArrayList<SliderItemBitmap> = ArrayList()

    private var uidString = ""

    private var filePathList = ArrayList<Uri>()

    private lateinit var drawerLayout: DrawerLayout

    private lateinit var navView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        drawerLayout = binding.drawerLayout

        navView = binding.navView

        navView.setNavigationItemSelectedListener(this)

        navView.menu.getItem(1).isChecked = true

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
            addReportData()
        }

        if (intent.extras != null) {
            val bundle = intent.getBundleExtra("location")
            val latitude = bundle?.getDouble(Const.LATITUDE)!!
            val longitude = bundle.getDouble(Const.LONGITUDE)

            reportData[Const.POSITION] = GeoPoint(latitude, longitude)
        }
    }
    private fun addReportData() {

        if (binding.infoId.text.toString() == "") {
            Toast.makeText(this, "Masukkan keterangan", Toast.LENGTH_SHORT).show()
            return
        }

        if (sliderItems.size == 0) {
            Toast.makeText(this, "Ambil gambar", Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Uploading...")
        progressDialog.show()
        uploadImage()

        reportData[Const.NOTE] = binding.infoId.text.toString()
        reportData[Const.LASTUPDATE] = Const.currentTimestamp

        firestore.collection("report")
            .add(reportData)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "berhasil upload", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                progressDialog.dismiss()
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

        if (sliderItems.size > 0) {
            binding.noImageLayout.visibility = View.INVISIBLE
            binding.imgLayout.visibility = View.VISIBLE

            val viewPager2 = binding.viewPagerImageSlider

            viewPager2.adapter = SliderAdapter2(sliderItems, viewPager2)

            viewPager2.clipToPadding = false
            viewPager2.clipChildren = false
            viewPager2.offscreenPageLimit = 3
            viewPager2.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

            val compositePageTransformer = CompositePageTransformer()
            compositePageTransformer.addTransformer(MarginPageTransformer(40))

            compositePageTransformer.addTransformer { page, position ->
                val r = 1 - abs(position)
                page.scaleY = 0.85f + r * 0.15f
            }

            viewPager2.setPageTransformer(compositePageTransformer)
        } else {
            binding.imgLayout.visibility = View.INVISIBLE
            binding.noImageLayout.visibility = View.VISIBLE
        }
    }

    private fun uploadImage() {

        uidString = UUID.randomUUID().toString()

        reportData[Const.IMAGEPATH] = "images/$uidString"

        for ((index, value) in filePathList.withIndex()) {
            val ref: StorageReference = mStorageRef.child("images/$uidString/$index")

            ref.putFile(value)
                .addOnSuccessListener {
                }
                .addOnFailureListener {
                }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.nav_home -> {
                startActivity(Intent(this, ViewMapsActivity::class.java))
                CustomIntent.customType(this, "left-to-right")
            }
            R.id.nav_feedback -> {
            }
            R.id.nav_settings -> {
                startActivity(Intent(this, SettingActivity::class.java))
                CustomIntent.customType(this, "left-to-right")
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)

        return true
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
                        showImage()
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

    override fun onBackPressed() {

        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        navView.menu.getItem(1).isChecked = true
        super.onResume()
    }

    override fun finish() {

        super.finish()
        CustomIntent.customType(this, "right-to-left")
    }
}
