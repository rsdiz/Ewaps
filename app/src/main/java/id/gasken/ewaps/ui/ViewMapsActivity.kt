package id.gasken.ewaps.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import id.gasken.ewaps.R
import id.gasken.ewaps.data.Points
import id.gasken.ewaps.databinding.ActivityViewMapsBinding
import id.gasken.ewaps.tool.Const
import id.gasken.ewaps.tool.viewBinding

class ViewMapsActivity :
    AppCompatActivity(),
    OnMapReadyCallback {
    private val binding: ActivityViewMapsBinding by viewBinding()

    private lateinit var mMap: GoogleMap
    private val db = FirebaseFirestore.getInstance()
    private val data: MutableList<Points> = mutableListOf()
    private lateinit var clusterManager: ClusterManager<PointItem>

    private val animationSlideDownFromHidden
        get() = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_down_from_hide)
    private val animationSlideUpToHidden
        get() = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_up_to_hide)
    private val animationSlideDownToHidden
        get() = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_down_to_hide)
    private val animationSlideUpFromHidden
        get() = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_up_from_hide)
    private val animationFadeIn
        get() = AnimationUtils.loadAnimation(applicationContext, R.anim.fade_in)
    private val animationFadeOut
        get() = AnimationUtils.loadAnimation(applicationContext, R.anim.fade_out)
    private val delayAnimSlide: Long = 300
    private val delayAnimFade: Long = 400

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db.collection(Const.DB_POINTS).get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val point = Points()
                    point.title = document.getString(Const.TITLE)!!
                    point.position = document.getGeoPoint(Const.POSITION)?.let { LatLng(it.latitude, it.longitude) }!!
                    point.note = document.getString(Const.NOTE)!!
                    point.lastUpdate = document.getTimestamp(Const.LASTUPDATE)!!
                    data.add(point)
                }

                val mapFragment = supportFragmentManager.findFragmentById(R.id.mapview) as SupportMapFragment
                mapFragment.getMapAsync(this)
            }
            .addOnFailureListener {
                Log.e("MAP", "Error occurred, cause ${it.message}")
            }

        binding.showSearch.setOnClickListener {
            if (it.tag == "show") {
                it.tag = "hide"
                binding.showSearch.setImageResource(R.drawable.ic_baseline_close_24)
                binding.layoutSearch.visibility = View.VISIBLE
                binding.layoutSearch.startAnimation(animationFadeIn)
                binding.buttonNavigasi.startAnimation(animationSlideDownToHidden)
                binding.buttonNavigasi.postDelayed(
                    {
                        binding.buttonNavigasi.visibility = View.GONE
                    },
                    delayAnimSlide
                )
            } else {
                it.tag = "show"
                binding.showSearch.setImageResource(R.drawable.ic_search_gray)
                binding.layoutSearch.startAnimation(animationFadeOut)
                binding.layoutSearch.postDelayed(
                    {
                        binding.layoutSearch.visibility = View.GONE
                    },
                    delayAnimFade
                )
                binding.buttonNavigasi.visibility = View.VISIBLE
                binding.buttonNavigasi.startAnimation(animationSlideUpFromHidden)
            }
        }

        binding.buttonSearchPoint.setOnClickListener {
            Toast.makeText(this, "Tombol Pencarian ditekan!", Toast.LENGTH_SHORT).show()
        }

        binding.buttonNavigasi.setOnClickListener {
            showNavigation(true)
            clearMaps()
        }

        binding.navigationCloseBtn.setOnClickListener {
            showNavigation(false)
            clearMaps()
            onMapReady(mMap)
        }

        binding.inputFirstLoc.setOnClickListener {
            binding.radiogroupFirstLocation.visibility = View.VISIBLE
        }

        binding.inputLastLoc.setOnClickListener {
            binding.radiogroupLastLocation.visibility = View.VISIBLE
        }

        binding.radiogroupFirstLocation.setOnCheckedChangeListener { group, checkedId ->
            group.clearCheck()
            group.visibility = View.GONE
            when (checkedId) {
                binding.buttonFirstMyloc.id -> {
                    searchMyLocation()
                }
                binding.buttonFirstPickOnMap.id -> {
                    pickLocationOnMap()
                }
            }
        }

        binding.radiogroupLastLocation.setOnCheckedChangeListener { group, checkedId ->
            group.clearCheck()
            group.visibility = View.GONE
            when (checkedId) {
                binding.buttonFirstMyloc.id -> {
                    searchMyLocation()
                }
                binding.buttonFirstPickOnMap.id -> {
                    pickLocationOnMap()
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.isTrafficEnabled = true
        mMap.isBuildingsEnabled = false
        mMap.isIndoorEnabled = false

        setupCluster(data)
    }

    private fun setupCluster(points: MutableList<Points>) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(-7.797068, 110.370529), 15f))

        clusterManager = ClusterManager(this, mMap)

        mMap.setOnCameraIdleListener(clusterManager)
        mMap.setOnMarkerClickListener(clusterManager)

        addPoints(points)
    }

    private fun addPoints(points: MutableList<Points>) {
        for (point in points) {
            val pointItem = PointItem(
                point.title,
                point.position,
                point.note
            )
            clusterManager.addItem(pointItem)
        }
    }

    private fun showNavigation(state: Boolean) {
        if (state) {
            binding.layoutNavigation.visibility = View.VISIBLE
            binding.layoutNavigation.startAnimation(animationSlideDownFromHidden)
            binding.buttonNavigasi.startAnimation(animationSlideDownToHidden)
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    binding.buttonNavigasi.visibility = View.GONE
                },
                delayAnimSlide
            )
            binding.topBar.visibility = View.INVISIBLE
        } else {
            binding.layoutNavigation.startAnimation(animationSlideUpToHidden)
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    binding.layoutNavigation.visibility = View.GONE
                },
                delayAnimSlide
            )
            binding.buttonNavigasi.visibility = View.VISIBLE
            binding.buttonNavigasi.startAnimation(animationSlideUpFromHidden)
            binding.topBar.visibility = View.VISIBLE
        }
    }

    private fun searchMyLocation() {
        Toast.makeText(this, "Mencari Lokasi Saya", Toast.LENGTH_SHORT).show()
    }

    private fun pickLocationOnMap() {
        Toast.makeText(this, "Pilih lokasi di maps", Toast.LENGTH_SHORT).show()
    }

    private fun clearMaps() {
        mMap.clear()
        clusterManager.clearItems()
    }

    inner class PointItem(
        private val title: String,
        private val position: LatLng,
        private val snippet: String
    ) : ClusterItem {
        override fun getPosition(): LatLng = position
        override fun getTitle(): String = title
        override fun getSnippet(): String = snippet
    }
}
