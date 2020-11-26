package id.gasken.ewaps.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.* // ktlint-disable no-wildcard-imports
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.BounceInterpolator
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.* // ktlint-disable no-wildcard-imports
import com.google.android.gms.maps.* // ktlint-disable no-wildcard-imports
import com.google.android.gms.maps.model.* // ktlint-disable no-wildcard-imports
import com.google.android.gms.tasks.RuntimeExecutionException
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import id.gasken.ewaps.R
import id.gasken.ewaps.data.Points
import id.gasken.ewaps.databinding.ActivityViewMapsBinding
import id.gasken.ewaps.tool.Const
import id.gasken.ewaps.tool.DirectionParser
import id.gasken.ewaps.tool.viewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max

class ViewMapsActivity :
    AppCompatActivity(),
    OnMapReadyCallback,
    GoogleMap.OnMarkerClickListener,
    GoogleMap.OnMarkerDragListener {
    private val binding: ActivityViewMapsBinding by viewBinding()

    private lateinit var mMap: GoogleMap
    private val db = FirebaseFirestore.getInstance()
    private val data: MutableList<Points> = mutableListOf()
    private lateinit var clusterManager: ClusterManager<PointItem>
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
    private val delayAnimSlide: Long = 300
    private val delayAnimFade: Long = 400

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fetch data from firebase, then save data result to variable data
        db.collection(Const.DB_POINTS).get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val point = Points()
                    point.title = document.getString(Const.TITLE)!!
                    point.position = document.getGeoPoint(Const.POSITION)
                        ?.let { LatLng(it.latitude, it.longitude) }!!
                    point.note = document.getString(Const.NOTE)!!
                    point.lastUpdate = document.getTimestamp(Const.LASTUPDATE)!!
                    data.add(point)
                }

                val mapFragment =
                    supportFragmentManager.findFragmentById(R.id.mapview) as SupportMapFragment
                mapFragment.getMapAsync(this)
            }
            .addOnFailureListener {
                Log.e("MAP", "Error occurred, cause ${it.message}")
            }

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

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
            try { mFusedLocationProviderClient.flushLocations() } catch (e: Exception) {}
            mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
            clearMaps()
        }

        binding.navigationCloseBtn.setOnClickListener {
            showNavigation(false)
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
        mMap.setOnMarkerClickListener(this)

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
                    params.addRule(RelativeLayout.BELOW, R.id.topBar)
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                    binding.mapLayout.layoutParams = params
                },
                delayAnimSlide
            )
            binding.buttonNavigasi.visibility = View.VISIBLE
            binding.buttonNavigasi.startAnimation(animationSlideUpFromHidden)
            binding.topBar.visibility = View.VISIBLE
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
                                client.checkLocationSettings(builder.build()).addOnCompleteListener { task ->
                                    try {
                                        val state: LocationSettingsStates = task.result!!.locationSettingsStates
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

                                mFusedLocationProviderClient.requestLocationUpdates(requestSetting, locationUpdates, null)
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
                                                    .icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons("marker1", 100, 100)))
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
                                                    .icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons("marker1", 100, 100)))
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

        mMap.setOnMarkerDragListener(this)
        onMarkerDragEnd(marker)
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
            button.startAnimation(animationSlideUpFromHidden)
            button.postDelayed({ button.visibility = View.VISIBLE }, delayAnimSlide)
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

    override fun onMarkerClick(marker: Marker?): Boolean {
        if (marker == null) return false
        // Markers have a z-index that is settable and gettable.
        marker.zIndex += 1.0f
        Toast.makeText(
            this, "${marker.title} z-index set to ${marker.zIndex}",
            Toast.LENGTH_SHORT
        ).show()

        val handler = Handler(Looper.getMainLooper())
        val start = SystemClock.uptimeMillis()
        val duration = 1500

        val interpolator = BounceInterpolator()

        handler.post(object : Runnable {
            override fun run() {
                val elapsed = SystemClock.uptimeMillis() - start
                val t = max(
                    1 - interpolator.getInterpolation(elapsed.toFloat() / duration), 0f
                )
                marker.setAnchor(0.5f, 1.0f + 2 * t)

                // Post again 16ms later.
                if (t > 0.0) {
                    handler.postDelayed(this, 16)
                }
            }
        })

        return false
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
