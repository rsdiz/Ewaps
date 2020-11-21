package id.gasken.ewaps.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import id.gasken.ewaps.R
import id.gasken.ewaps.databinding.ActivityUserInputBinding
import java.io.FileNotFoundException
import java.io.IOException

class UserInputActivity : AppCompatActivity(), OnMapReadyCallback, OnMarkerDragListener {

    private lateinit var binding: ActivityUserInputBinding

    private lateinit var mMap: GoogleMap

    private val firestore = FirebaseFirestore.getInstance()

    private val mStorageRef = FirebaseStorage.getInstance().getReference()

    private val report_data: MutableMap<String, Any> = HashMap<String, Any>()

    private lateinit var filePath: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserInputBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkTabLayout()

        val mapFragment = (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment)

        mapFragment.getMapAsync(this)

        binding.cameraBtn.setOnClickListener {
//            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//            startActivityForResult(cameraIntent, 0)

            val mediaIntent = Intent().setType("*/*").setAction(Intent.ACTION_GET_CONTENT)
            startActivityForResult(Intent.createChooser(mediaIntent, "Select Picture"), 1)
        }

        binding.submitBtn.setOnClickListener {
            if (binding.infoId.text.toString() == "") {
                Toast.makeText(this, "keterangan kosong", Toast.LENGTH_SHORT).show()

            }else{
                addReportData()

            }
        }
    }

    private fun addReportData(){



        firestore.collection("report")
                .add(report_data)
                .addOnSuccessListener {
                    println("success")
                }
                .addOnFailureListener {
                    println("gagal")
                }

    }


    private fun checkTabLayout() {
        val homeBtn = binding.tablayout.homeBtn
        homeBtn.setColorFilter(ContextCompat.getColor(this, R.color.gray))

        val mapsBtn = binding.tablayout.mapsBtn
        mapsBtn.setColorFilter(ContextCompat.getColor(this, R.color.gray))

        val userInputBtn = binding.tablayout.userInputBtn
        userInputBtn.setColorFilter(ContextCompat.getColor(this, R.color.whiteGreen))

//        if (TabLayout.userInputState){
//
//        }
    }



    override fun onMarkerDragStart(p0: Marker?) {
//        TODO("Not yet implemented")
    }

    override fun onMarkerDrag(p0: Marker?) {
//        TODO("Not yet implemented")
    }

    override fun onMarkerDragEnd(marker: Marker?) {
        if (marker != null){
            println(marker.position)
            report_data.put("location", marker.position)
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
                    // OK Result
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show()
                }
            }

            1 -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        try {

                            filePath = data.data!!

                            val bitmap : Bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, filePath)

                            binding.imageView.setImageBitmap(bitmap)

                        }catch (e: IOException){
                            Toast.makeText(this, "Error Occured", Toast.LENGTH_SHORT).show()

                        }catch (e: FileNotFoundException) {
                            Toast.makeText(this, "File Not Found", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

