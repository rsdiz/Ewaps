package id.gasken.ewaps.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.* // ktlint-disable no-wildcard-imports
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.* // ktlint-disable no-wildcard-imports
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.* // ktlint-disable no-wildcard-imports
import com.google.android.gms.maps.* // ktlint-disable no-wildcard-imports
import com.google.android.gms.maps.model.* // ktlint-disable no-wildcard-imports
import com.google.android.gms.tasks.RuntimeExecutionException
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import id.gasken.ewaps.R
import id.gasken.ewaps.data.Points
import id.gasken.ewaps.databinding.ActivityViewMapsBinding
import id.gasken.ewaps.tool.* // ktlint-disable no-wildcard-imports
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import maes.tech.intentanim.CustomIntent
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.* // ktlint-disable no-wildcard-imports
import kotlin.collections.ArrayList

class ViewMapsActivity :
    AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener,
    OnMapReadyCallback {
    private val binding: ActivityViewMapsBinding by viewBinding()

    private lateinit var mMap: GoogleMap
    private val db = FirebaseFirestore.getInstance()

    // Create a storage reference from our app
    private val storage = FirebaseStorage.getInstance()
    private val data: MutableList<Points> = mutableListOf()
    private lateinit var clusterManager: ClusterManager<PointItem>
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    // Variable for place picker
    private val tag = "ViewMapsActivity"
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient

    // Variable for currently location of the devices
    private lateinit var mLastKnownLocation: Location
    private var mLocationPermissionGranted: Boolean = true
    private lateinit var locationManager: LocationManager

    // Variable for saving location
    private var firstLocation: Points = Points()
    private var secondLocation: Points = Points()
    private var isLocationAlreadySet = arrayOf(false, false)

    // Variable for active drag location
    private var activeLocation = 0

    // Variable for check
    private var isUserSetReportLocation = false
    private var isNavigationShow = false
    private var stateBottomSheet = BottomSheetBehavior.STATE_HIDDEN
    private var markerSelected = -1
    private var isGpsLocked = false

    // Variable for Navigation View
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    private var addresses: MutableList<android.location.Address> = arrayListOf()

    private val requestSetting = LocationRequest.create().apply {
        fastestInterval = 10000
        interval = 10000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        smallestDisplacement = 1.0F
    }

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
    private val animationSlideRightToHidden
        get() = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_right_to_hide)
    private val animationSlideLeftFromHidden
        get() = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_left_from_hide)
    private val delayAnimSlide: Long = 300
    private val delayAnimFade: Long = 400

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        drawerLayout = binding.drawerLayout
        navView = binding.navView

        navView.setNavigationItemSelectedListener(this)
        navView.menu.getItem(0).isChecked = true

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        if (savedInstanceState == null) {
            getLocation()
        }

        fetchDataFromDB()

        setBindingAction()
    }

    /**
     * Set Everything on layout has action
     */
    private fun setBindingAction() {
        val bottomSheet: LinearLayout = binding.layoutBottomSheet
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

        bottomSheetBehavior.addBottomSheetCallback(object :
                BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    stateBottomSheet = newState
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            }
        )

        binding.buttonNavigasi.setOnClickListener {
            showNavigation(true)
            showButtonAcceleration(true)
            try {
                mFusedLocationProviderClient.flushLocations()
            } catch (e: Exception) {
            }
            mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
            clearMaps()
        }

        binding.iconMenu.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        binding.navigationCloseBtn.setOnClickListener {
            showNavigation(false)
            showButtonAcceleration(false)
            resetNavigationForm()
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
            group.visibility = View.GONE
            clearCheckedRadioGroup(group.id)
            when (checkedId) {
                binding.buttonFirstMyloc.id -> {
                    searchMyLocation(1)
                }
                binding.buttonFirstPickOnMap.id -> {
                    pickLocationOnMap(1)
                }
            }
        }

        binding.radiogroupLastLocation.setOnCheckedChangeListener { group, checkedId ->
            group.visibility = View.GONE
            clearCheckedRadioGroup(group.id)
            when (checkedId) {
                binding.buttonLastMyloc.id -> {
                    searchMyLocation(2)
                }
                binding.buttonLastPickOnMap.id -> {
                    pickLocationOnMap(2)
                }
            }
        }

        binding.buttonReport.setOnClickListener {
            isUserSetReportLocation = true
            setTopic("Laporkan Titik Rawan")
            showButtonEmergencyCall(false)
            showButtonReport(false)
            showButtonNextReport(true)
            pickReportLocationOnMap()
        }

        binding.buttonEmergencyCall.setOnClickListener {
            intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + "0274368238"))
            startActivity(intent)
        }

        binding.buttonSearch.setOnClickListener {
            if (it.tag == "search") {
                it.tag = "close"
                binding.buttonSearch.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_baseline_close_24))
                binding.layoutSearch.visibility = View.VISIBLE
                binding.layoutSearch.startAnimation(animationSlideDownFromHidden)
            } else {
                it.tag = "search"
                binding.buttonSearch.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_search_gray))
                binding.layoutSearch.startAnimation(animationSlideUpToHidden)
                binding.layoutSearch.postDelayed(
                    {
                        binding.layoutSearch.visibility = View.GONE
                    },
                    delayAnimSlide
                )
            }
        }

        binding.playVideoBtn.setOnClickListener {
            playVideo()
        }
    }

    /**
     * Fetch data from firebase, then save data result to variable data
     */
    private fun fetchDataFromDB() {
        db.collection(Const.DB_POINTS).get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val point = Points()
                    point.title = document.getString(Const.TITLE)!!
                    point.position = document.getGeoPoint(Const.POSITION)
                        ?.let { LatLng(it.latitude, it.longitude) }!!
                    point.note = document.getString(Const.NOTE)!!
                    point.lastUpdate = document.getTimestamp(Const.LASTUPDATE)!!
                    point.imagePath = document.getString(Const.IMAGEPATH) ?: ""
                    data.add(point)
                }

                db.collection(Const.DB_REPORT).get()
                    .addOnSuccessListener { result2 ->
                        for (document in result2) {
                            val point = Points()
                            point.title = "Lokasi Rawan"
                            point.position = document.getGeoPoint(Const.POSITION)
                                ?.let { LatLng(it.latitude, it.longitude) }!!
                            point.note = document.getString(Const.NOTE)!!
                            point.lastUpdate = document.getTimestamp(Const.LASTUPDATE)!!
                            point.imagePath = document.getString(Const.IMAGEPATH) ?: ""
                            point.videoPath = document.getString(Const.VIDEOPATH) ?: ""
                            data.add(point)
                        }

                        val mapFragment =
                            supportFragmentManager.findFragmentById(R.id.mapview) as SupportMapFragment
                        mapFragment.getMapAsync(this)
                    }
                    .addOnFailureListener {
                        Log.e(tag, "Error occurred, cause ${it.message}")
                    }
            }
            .addOnFailureListener {
                Log.e(tag, "Error occurred, cause ${it.message}")
            }
    }

    private fun getLocation() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                44
            )
            return
        }
        mFusedLocationProviderClient.lastLocation
            .addOnCompleteListener {

                if (it.result != null) {
                    mLastKnownLocation = it.result

                    val geocoder = Geocoder(this, Locale.getDefault())

                    addresses = geocoder.getFromLocation(
                        mLastKnownLocation.latitude, mLastKnownLocation.longitude, 1
                    )

                    isGpsLocked = true
                } else {
                    showWarningGPS()
                }
            }
            .addOnFailureListener {
                Log.d("ViewMapsActivity", "===================== ERROR GPS")
            }
    }

    private fun showButtonAcceleration(state: Boolean) {
        if (state) {
            binding.buttonSpeedMeter.visibility = View.VISIBLE
            binding.buttonSpeedMeter.startAnimation(animationSlideUpFromHidden)
        } else {
            binding.buttonSpeedMeter.startAnimation(animationSlideDownToHidden)
            binding.buttonSpeedMeter.visibility = View.GONE
        }
    }

    private fun setTopic(title: String = getString(R.string.titikRawanTopic)) {
        binding.topic.text = title
    }

    private fun showButtonReport(state: Boolean) {
        if (state) {
            binding.buttonReport.visibility = View.VISIBLE
            binding.buttonReport.startAnimation(animationSlideLeftFromHidden)
        } else {
            binding.buttonReport.startAnimation(animationSlideRightToHidden)
            binding.buttonReport.visibility = View.GONE
        }
    }

    private fun showButtonNextReport(state: Boolean) {
        if (state) {
            binding.buttonNextReport.visibility = View.VISIBLE
            binding.buttonNextReport.startAnimation(animationSlideUpFromHidden)
        } else {
            binding.buttonNextReport.startAnimation(animationSlideDownToHidden)
            binding.buttonNextReport.postDelayed(
                {
                    binding.buttonNextReport.visibility = View.GONE
                },
                delayAnimSlide
            )
        }
    }

    private fun pickReportLocationOnMap() {
        showButtonNavigation(false)

        val marker: Marker

        if (isGpsLocked) {

            val latLng = LatLng(addresses[0].latitude, addresses[0].longitude)

            marker = mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Lokasi yang ingin dilaporkan")
                    .draggable(true)
                    .icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons("flag_marker", 100, 100)))
            )
            mMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    latLng,
                    12F
                )
            )
        } else {
            marker = mMap.addMarker(
                MarkerOptions()
                    .position(LatLng(-7.797068, 110.370529))
                    .title("Lokasi yang ingin dilaporkan")
                    .draggable(true)
                    .icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons("flag_marker", 100, 100)))
            )
            mMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(-7.797068, 110.370529),
                    12F
                )
            )
        }

        mMap.setOnMarkerDragListener(PickReportLocation())
        PickReportLocation().onMarkerDragEnd(marker)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.isTrafficEnabled = true
        mMap.isBuildingsEnabled = false
        mMap.isIndoorEnabled = false

        setupCluster(data)
    }

    private fun setupCluster(points: MutableList<Points>) {

        Log.d("ViewMapsActivity", "===================== isGpsLocked: $isGpsLocked")

        if (isGpsLocked) {
            val latLng = LatLng(addresses[0].latitude, addresses[0].longitude)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        } else {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(-7.797068, 110.370529), 15f))
        }

        clusterManager = ClusterManager(this, mMap)
        addPoints(points)

        clusterManager.markerCollection.setOnMarkerClickListener(MarkerAction())
        mMap.setOnCameraIdleListener(clusterManager)
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
        isNavigationShow = state
        val params = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        if (state) {
            binding.layoutNavigation.visibility = View.VISIBLE
            binding.layoutNavigation.startAnimation(animationSlideDownFromHidden)
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    params.addRule(RelativeLayout.BELOW, R.id.layoutNavigation)
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                    binding.mapLayout.layoutParams = params
                },
                delayAnimSlide
            )
            binding.topBar.visibility = View.INVISIBLE
        } else {
            binding.layoutNavigation.startAnimation(animationSlideUpToHidden)
            params.addRule(RelativeLayout.BELOW, R.id.topBar)
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
            binding.mapLayout.layoutParams = params
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    binding.layoutNavigation.visibility = View.GONE
                },
                delayAnimSlide
            )
            binding.topBar.visibility = View.VISIBLE
        }
        showButtonNavigation(!state)
        showButtonEmergencyCall(!state)
        showButtonReport(!state)
    }

    private fun showButtonNavigation(state: Boolean) {
        if (state) {
            binding.buttonNavigasi.visibility = View.VISIBLE
            binding.buttonNavigasi.startAnimation(animationSlideUpFromHidden)
        } else {
            binding.buttonNavigasi.startAnimation(animationSlideDownToHidden)
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    binding.buttonNavigasi.visibility = View.GONE
                },
                delayAnimSlide
            )
        }
    }

    private fun showButtonEmergencyCall(state: Boolean) {
        if (state) {
            binding.buttonEmergencyCall.visibility = View.VISIBLE
            binding.buttonEmergencyCall.startAnimation(animationSlideLeftFromHidden)
        } else {
            binding.buttonEmergencyCall.startAnimation(animationSlideRightToHidden)
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    binding.buttonEmergencyCall.visibility = View.GONE
                },
                delayAnimSlide
            )
        }
    }

    private fun searchMyLocation(locationId: Int) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        } else {
            mLocationPermissionGranted = true
        }

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            try {
                showButtonAcceleration(false)
                activeLocation = locationId
                if (mLocationPermissionGranted) {
                    val locationResult: Task<Location> = mFusedLocationProviderClient.lastLocation
                    locationResult.addOnCompleteListener { locRes ->
                        if (locRes.isSuccessful) {
                            if (locRes.result != null) {
                                mLastKnownLocation = locRes.result
                                mMap.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(
                                            mLastKnownLocation.latitude,
                                            mLastKnownLocation.longitude
                                        ),
                                        15F
                                    )
                                )
                            } else {
                                val requestCheckState = 12300
                                val builder = LocationSettingsRequest.Builder()
                                    .addLocationRequest(requestSetting)
                                val client = LocationServices.getSettingsClient(this)
                                client.checkLocationSettings(builder.build())
                                    .addOnCompleteListener { task ->
                                        try {
                                            val state: LocationSettingsStates =
                                                task.result!!.locationSettingsStates
                                            Log.d(tag, task.result!!.toString())
                                            Log.e(
                                                "LOG",
                                                "LocationSettings: \n" +
                                                    " GPS present: ${state.isGpsPresent} \n" +
                                                    " GPS usable: ${state.isGpsUsable} \n" +
                                                    " Location present: " +
                                                    "${state.isLocationPresent} \n" +
                                                    " Location usable: " +
                                                    "${state.isLocationUsable} \n" +
                                                    " Network Location present: " +
                                                    "${state.isNetworkLocationPresent} \n" +
                                                    " Network Location usable: " +
                                                    "${state.isNetworkLocationUsable} \n"
                                            )
                                        } catch (e: RuntimeExecutionException) {
                                            Log.d(tag, "Error occured!")
                                            if (e.cause is ResolvableApiException)
                                                (e.cause as ResolvableApiException).startResolutionForResult(
                                                    this,
                                                    requestCheckState
                                                )
                                        }
                                    }

                                val locationUpdates = object : LocationCallback() {
                                    override fun onLocationResult(result: LocationResult?) {
                                        if (result != null) {
                                            mMap.animateCamera(
                                                CameraUpdateFactory.newLatLngZoom(
                                                    LatLng(
                                                        result.locations.last().latitude,
                                                        result.locations.last().longitude
                                                    ),
                                                    15F
                                                )
                                            )
                                        }
                                    }
                                }

                                mFusedLocationProviderClient.requestLocationUpdates(
                                    requestSetting,
                                    locationUpdates,
                                    null
                                )
                                mFusedLocationProviderClient.removeLocationUpdates(locationUpdates)
                            }

                            when (activeLocation) {
                                1 ->
                                    binding.inputFirstLoc.text =
                                        getString(R.string.text_my_location)
                                2 ->
                                    binding.inputLastLoc.text =
                                        getString(R.string.text_my_location)
                            }

                            showMyLocationOnMap(true)

                            binding.buttonSelectLocation.let { button ->
                                button.visibility = View.VISIBLE
                                button.startAnimation(animationFadeIn)
                                button.setOnClickListener {
                                    button.startAnimation(animationFadeOut)
                                    button.postDelayed(
                                        {
                                            button.visibility = View.GONE
                                        },
                                        delayAnimFade
                                    )
                                    when (activeLocation) {
                                        1 -> {
                                            isLocationAlreadySet[0] = true
                                            firstLocation = Points(
                                                "Lokasi Awal",
                                                LatLng(
                                                    mLastKnownLocation.latitude,
                                                    mLastKnownLocation.longitude
                                                )
                                            )
                                            mMap.addMarker(
                                                MarkerOptions()
                                                    .title(firstLocation.title)
                                                    .position(firstLocation.position)
                                                    .icon(
                                                        BitmapDescriptorFactory.fromBitmap(
                                                            resizeMapIcons("marker1", 100, 100)
                                                        )
                                                    )
                                            )
                                            showMyLocationOnMap(false)
                                        }
                                        2 -> {
                                            isLocationAlreadySet[1] = true
                                            secondLocation = Points(
                                                "Lokasi Tujuan",
                                                LatLng(
                                                    mLastKnownLocation.latitude,
                                                    mLastKnownLocation.longitude
                                                )
                                            )
                                            mMap.addMarker(
                                                MarkerOptions()
                                                    .title(secondLocation.title)
                                                    .position(secondLocation.position)
                                                    .icon(
                                                        BitmapDescriptorFactory.fromBitmap(
                                                            resizeMapIcons("marker1", 100, 100)
                                                        )
                                                    )
                                            )
                                            showMyLocationOnMap(false)
                                        }
                                    }
                                    it.startAnimation(animationFadeOut)
                                    it.postDelayed({ it.visibility = View.GONE }, delayAnimFade)
                                }
                                doRequestDirection()
                            }
                        } else {
                            Toast.makeText(this, "Lokasi tidak dapat ditemukan!", Toast.LENGTH_LONG)
                                .show()
                        }
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Tidak mendapat ijin untuk mengakses lokasi",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.d(tag, "Persmission not granted!")
                }
            } catch (ex: Exception) {
                Log.e(tag, "Exception: ${ex.message}")
            } else {
            showWarningGPS()
        }
    }

    /**
     * Request API from Google Direction API
     */
    private fun doRequestDirection() {
        if (isLocationAlreadySet.contentEquals(arrayOf(true, true))) {
            println(firstLocation)
            println(secondLocation)
            CoroutineScope(Dispatchers.IO).launch {
                var responseString: String? = null
                try {
                    println(getRequestedUrl(firstLocation.position, secondLocation.position))
                    responseString = requestDirection(
                        getRequestedUrl(
                            firstLocation.position,
                            secondLocation.position
                        )
                    )
                    println(responseString)
                } catch (e: Exception) {
                    Log.e(tag, "Request Direction Error!")
                    e.printStackTrace()
                }

                var routes: List<List<HashMap<String, String>>>? = null
                val jsonObject: JSONObject?
                try {
                    jsonObject = JSONObject(responseString!![0].toString())
                    println(jsonObject)
                    routes = DirectionParser().parse(jsonObject)
                } catch (e: JSONException) {
                    e.printStackTrace()
                } catch (e: IndexOutOfBoundsException) {
                    Log.e(tag, "Error occured when fetch JSON response from Google Direction API")
                    e.printStackTrace()
                    println(responseString!!)
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    if (routes != null) {
                        var points: ArrayList<LatLng>?
                        var polylineOptions: PolylineOptions? = null
                        for (path in routes) {
                            points = ArrayList()
                            polylineOptions = PolylineOptions()
                            for (point in path) {
                                if (!point["lat"].isNullOrBlank() && !point["lng"].isNullOrBlank())
                                    points.add(
                                        LatLng(
                                            point["lat"]?.toDouble()!!,
                                            point["lng"]?.toDouble()!!
                                        )
                                    )
                            }
                            polylineOptions.addAll(points)
                            polylineOptions.width(15F)
                            polylineOptions.color(Color.BLUE)
                            polylineOptions.geodesic(true)
                        }
                        if (polylineOptions != null)
                            mMap.addPolyline(polylineOptions)
                        else
                            Toast.makeText(
                                this@ViewMapsActivity,
                                "Direction not found",
                                Toast.LENGTH_LONG
                            ).show()
                    }
                }
            }
        }
    }

    /**
     * Update UI maps that show the user location
     *
     * @param boolean
     */
    @SuppressLint("MissingPermission")
    private fun showMyLocationOnMap(boolean: Boolean) {
        try {
            mMap.isMyLocationEnabled = boolean
        } catch (e: Exception) {
        }
        mMap.uiSettings.isMyLocationButtonEnabled = boolean
    }

    private fun pickLocationOnMap(selectLocation: Int) {
        showButtonAcceleration(false)
        activeLocation = selectLocation

        when (activeLocation) {
            1 ->
                binding.inputFirstLoc.text =
                    getString(R.string.text_select_on_map)
            2 ->
                binding.inputLastLoc.text =
                    getString(R.string.text_select_on_map)
        }

        val marker = mMap.addMarker(
            MarkerOptions()
                .position(LatLng(-7.797068, 110.370529))
                .title(
                    if (activeLocation == 1) "Pilih Lokasi Awal" else "Pilih Lokasi Tujuan"
                )
                .draggable(true)
                .icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons("marker1", 100, 100)))
        )

        mMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(-7.797068, 110.370529),
                15F
            )
        )

        mMap.setOnMarkerDragListener(PickNavigationLocation())
        PickNavigationLocation().onMarkerDragEnd(marker)
    }

    /**
     * Convert BitmapDescriptor to Bitmap with specific size
     *
     * @param iconName name resource in drawable
     * @param width convert bitmap to this width
     * @param height convert bitmap to this height
     * @return Bitmap with size from user input
     */
    private fun resizeMapIcons(iconName: String?, width: Int, height: Int): Bitmap? {
        val imageBitmap = BitmapFactory.decodeResource(
            resources,
            resources.getIdentifier(
                iconName, "drawable",
                application.packageName
            )
        )
        return Bitmap.createScaledBitmap(imageBitmap, width, height, false)
    }

    /**
     * Show Custom Alert with custom layout for give information that user not activated GPS
     */
    private fun showWarningGPS() {
        val layoutDialog = LayoutInflater.from(this).inflate(
            R.layout.layout_warning_enable_gps,
            null
        )
        val builder = AlertDialog.Builder(this, R.style.ThemeOverlay_AppCompat_Dialog_Alert)
            .setView(layoutDialog)
        val alertDialog = builder.show()
        alertDialog.setCanceledOnTouchOutside(false)
        layoutDialog.findViewById<Button>(R.id.buttonKembali).setOnClickListener {
            alertDialog.dismiss()
        }
        layoutDialog.findViewById<Button>(R.id.buttonHidupkan).setOnClickListener {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            alertDialog.dismiss()
        }
    }

    /**
     * Clear everything on the map
     */
    private fun clearMaps() {
        mMap.clear()
        clusterManager.clearItems()
    }

    /**
     * Reset everything on navigation layout
     */
    private fun resetNavigationForm() {
        binding.inputFirstLoc.text = getText(R.string.text_input_first_location)
        binding.inputLastLoc.text = getText(R.string.text_input_last_location)
        clearCheckedRadioGroup(binding.radiogroupFirstLocation.id)
        clearCheckedRadioGroup(binding.radiogroupLastLocation.id)
        firstLocation = Points()
        secondLocation = Points()
        activeLocation = 0
        isLocationAlreadySet = arrayOf(false, false)
    }

    /**
     * Clear the checked radio button on navigation layout
     *
     * @param id from RadioGroup
     */
    private fun clearCheckedRadioGroup(id: Int) {
        when (id) {
            binding.radiogroupFirstLocation.id -> {
                binding.buttonFirstMyloc.isChecked = false
                binding.buttonFirstPickOnMap.isChecked = false
            }
            binding.radiogroupLastLocation.id -> {
                binding.buttonLastMyloc.isChecked = false
                binding.buttonLastPickOnMap.isChecked = false
            }
        }
    }

    /**
     * on click play video button
     */
    private fun playVideo() {

        if (data[markerSelected].videoPath == "") {
            Toast.makeText(this, "Video tidak tersedia", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, VideoPreviewActivity::class.java)
        intent.putExtra("online", true)
        intent.putExtra(Const.VIDEOPATH, data[markerSelected].videoPath)
        startActivity(intent)
    }

    /**
     * Request permission to user
     *
     * @param permission what permission to requested
     */
    private fun requestPermission(permission: String) {
        if (ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(permission),
                200
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        mLocationPermissionGranted = false
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    mLocationPermissionGranted = true
            }
        }
    }

    /**
     * Function for create url for Direction API to get route from firstLocation to secondLocation
     *
     * @param origin First location
     * @param destination Last location
     * @return URL to Google Direction API
     */
    private fun getRequestedUrl(origin: LatLng, destination: LatLng): String {
        val strOrigin = "origin=${origin.latitude},${origin.longitude}"
        val strDestination = "destination=${destination.latitude},${destination.longitude}"
        val sensor = "sensor=false"
        val mode = "mode=driving"

        val params = "$strOrigin&$strDestination&$sensor&$mode"
        val output = "json"
        val apiKey = "&key=" + getString(R.string.google_api_key)

        return "https://maps.googleapis.com/maps/api/directions/$output?$params$apiKey"
    }

    /**
     * Request direction from Google Direction API
     *
     * @param requestedUrl
     * @return JSON data routes/direction
     */
    private fun requestDirection(requestedUrl: String): String? {
        var responseString: String? = null
        var inputStream: InputStream? = null
        var httpUrlConnection: HttpURLConnection? = null
        try {
            val url = URL(requestedUrl)
            httpUrlConnection = url.openConnection() as HttpURLConnection
            httpUrlConnection.connect()

            inputStream = httpUrlConnection.inputStream
            val reader = InputStreamReader(inputStream)
            val buffReader = BufferedReader(reader)

            val stringBuffer = StringBuffer()
            var line: String? = null
            while (line != null) {
                line = buffReader.readLine()
                stringBuffer.append(line)
            }

            responseString = stringBuffer.toString()
            buffReader.close()
            reader.close()
        } catch (e: Exception) {
            Log.d(tag, "Error occured! cause: ${e.message}")
            e.printStackTrace()
        } finally {
            if (inputStream != null)
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
        }
        httpUrlConnection?.disconnect()
        return responseString
    }

    override fun onBackPressed() {
        if (isUserSetReportLocation) {
            isUserSetReportLocation = false
            setTopic()
            showButtonReport(true)
            showButtonEmergencyCall(true)
            showButtonNextReport(false)
            showButtonNavigation(true)
            clearMaps()
            onMapReady(mMap)
            return
        }

        if (isNavigationShow) {
            binding.navigationCloseBtn.performClick()
            return
        }

        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
            return
        }

        super.onBackPressed()
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

    inner class PickNavigationLocation : GoogleMap.OnMarkerDragListener {
        override fun onMarkerDragStart(marker: Marker?) {
            binding.buttonSelectLocation.let {
                it.startAnimation(animationSlideDownToHidden)
                it.postDelayed({ it.visibility = View.GONE }, delayAnimSlide)
            }
        }

        override fun onMarkerDrag(marker: Marker?) {
        }

        override fun onMarkerDragEnd(marker: Marker) {
            binding.buttonSelectLocation.let { button ->
                button.visibility = View.VISIBLE
                button.startAnimation(animationSlideUpFromHidden)
                button.setOnClickListener {
                    button.startAnimation(animationFadeOut)
                    button.postDelayed(
                        {
                            it.visibility = View.GONE
                        },
                        delayAnimFade
                    )
                    when (activeLocation) {
                        1 -> {
                            isLocationAlreadySet[0] = true
                            firstLocation =
                                Points(
                                    position = LatLng(
                                        marker.position.latitude,
                                        marker.position.longitude
                                    ),
                                    title = "Lokasi Awal"
                                )
                        }
                        2 -> {
                            isLocationAlreadySet[1] = true
                            secondLocation =
                                Points(
                                    position = LatLng(
                                        marker.position.latitude,
                                        marker.position.longitude
                                    ),
                                    title = "Lokasi Tujuan"
                                )
                        }
                    }
                    doRequestDirection()
                    activeLocation = 0
                }
            }
        }
    }

    inner class PickReportLocation : GoogleMap.OnMarkerDragListener {
        override fun onMarkerDragStart(marker: Marker?) {
            binding.buttonNextReport.let {
                it.startAnimation(animationSlideDownToHidden)
                it.postDelayed({ it.visibility = View.GONE }, delayAnimSlide)
            }
        }

        override fun onMarkerDrag(marker: Marker?) {
        }

        override fun onMarkerDragEnd(marker: Marker) {
            binding.buttonNextReport.let { button ->
                button.visibility = View.VISIBLE
                button.startAnimation(animationSlideUpFromHidden)
                button.setOnClickListener {
                    val bundle = Bundle()
                    bundle.putDouble(Const.LATITUDE, marker.position.latitude)
                    bundle.putDouble(Const.LONGITUDE, marker.position.longitude)
                    intent = Intent(this@ViewMapsActivity, UserInputActivity::class.java)
                    intent.putExtra("location", bundle)
                    CustomIntent.customType(this@ViewMapsActivity, "left-to-right")
                    startActivity(intent)
                }
            }
        }
    }

    inner class MarkerAction : GoogleMap.OnMarkerClickListener {
        private val storageRef = storage.reference

        override fun onMarkerClick(marker: Marker): Boolean {
            run(marker)
            return true
        }

        private fun run(marker: Marker) {
            var i = 0
            data.iterator().forEach { item ->
                if (marker.position.latitude == item.position.latitude && marker.position.longitude == item.position.longitude) {
                    markerSelected = i
                    return@forEach
                } else {
                    i++
                }
            }

            when (stateBottomSheet) {
                BottomSheetBehavior.STATE_HIDDEN, BottomSheetBehavior.STATE_COLLAPSED -> {

                    try {
                        if (data[markerSelected].imagePath != "") {
                            findViewById<ImageView>(R.id.imagePoint).apply {
                                val imageResource =
                                    storageRef.child(data[markerSelected].imagePath).child("0")
                                GlideApp.with(this@ViewMapsActivity)
                                    .load(imageResource)
                                    .placeholder(R.drawable.flag_marker)
                                    .centerCrop()
                                    .into(this)
                            }
                        } else {
                            findViewById<ImageView>(R.id.imagePoint).apply {
                                GlideApp.with(this@ViewMapsActivity)
                                    .load(R.drawable.flag_marker)
                                    .centerCrop()
                                    .into(this)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    findViewById<TextView>(R.id.titlePoint).apply {
                        this.text = data[markerSelected].title
                    }
                    findViewById<TextView>(R.id.notePoint).apply {
                        this.text = data[markerSelected].note
                    }
                    findViewById<Button>(R.id.playVideoBtn).apply {
                        if (data[markerSelected].videoPath != "") {
                            this.text = "Putar Video"
                            this.visibility = View.VISIBLE
                        } else {
                            this.visibility = View.GONE
                        }
                    }

                    BottomSheetBehavior.STATE_HALF_EXPANDED.let {
                        bottomSheetBehavior.state = it
                        stateBottomSheet = it
                    }
                }

                BottomSheetBehavior.STATE_HALF_EXPANDED, BottomSheetBehavior.STATE_EXPANDED -> {
                    if (markerSelected == -1) {
                        return
                    }

                    BottomSheetBehavior.STATE_HIDDEN.let {
                        bottomSheetBehavior.state = it
                        stateBottomSheet = it
                        markerSelected = -1
                    }
                }
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.nav_home -> {
            }
            R.id.nav_feedback -> {
                startActivity(Intent(this, UserInputActivity::class.java))
                CustomIntent.customType(this, "left-to-right")
            }
            R.id.nav_settings -> {
                startActivity(Intent(this, SettingActivity::class.java))
                CustomIntent.customType(this, "left-to-right")
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)

        return true
    }

    override fun onResume() {
        navView.menu.getItem(0).isChecked = true
        getLocation()
        super.onResume()
    }

    override fun finish() {

        super.finish()
        CustomIntent.customType(this, "right-to-left")
    }
}
