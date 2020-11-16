package id.gasken.ewaps.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.CameraUpdateFactory
import id.gasken.ewaps.R
import id.gasken.ewaps.databinding.ActivityUserInputBinding
import java.io.IOException

class UserInputActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityUserInputBinding

    private lateinit var mMap: GoogleMap

//    private val pic_id = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserInputBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment)

        mapFragment.getMapAsync(this)

        binding.cameraBtn.setOnClickListener {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, 0)

//            val mediaIntent = Intent(Intent.ACTION_GET_CONTENT)
//            startActivityForResult(Intent.createChooser(mediaIntent, "Select Picture"), 1)
        }


    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap


        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))

        mMap.setMinZoomPreference(15.0F)

        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)



        when(requestCode){
            0 -> {
                if (resultCode == Activity.RESULT_OK){

                }else if (resultCode == Activity.RESULT_CANCELED)  {
                    Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show();
                }
            }

            1 -> {
                if (resultCode == Activity.RESULT_OK){
                    if(data != null){
                        try {
                            val bitmap : Bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, data.data)
                        }catch (e: IOException){
                            Toast.makeText(this, "Error Occured", Toast.LENGTH_SHORT).show();
                        }
                    }
                }else if (resultCode == Activity.RESULT_CANCELED)  {
                    Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


}