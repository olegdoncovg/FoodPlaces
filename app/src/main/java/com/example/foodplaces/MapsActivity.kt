package com.example.foodplaces

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment
import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.geometry.SpatialReferences
import com.esri.arcgisruntime.loadable.LoadStatus
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult
import com.esri.arcgisruntime.tasks.geocode.LocatorTask
import com.example.foodplaces.MapUtil.Companion.addMarker
import com.example.foodplaces.MapUtil.Companion.hasVisiblePlaces
import com.example.foodplaces.realm.PlaceRealm
import com.example.foodplaces.realm.RealmManager
import com.example.foodplaces.realm.RealmManager.OnResultListener
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.function.Consumer

class MapsActivity : FragmentActivity(), OnMapReadyCallback {
    companion object {
        private val TAG = MapsActivity::class.java.simpleName
        private const val DEFAULT_ZOOM = 14f
        private const val DEFAULT_ZOOM_FOCUS = 17f
        private const val TIME_NO_CAMERA_ACTION_FOR_SEARCH = 1000
        private const val DEFAULT_POI_ADDRESS = "food"
        private const val MAX_RESULTS = 20
        private const val CAMERA_EDGE_PADDING_PX = 100
        private const val TIMER_FPS = 40
    }

    private val reqPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION)
    private var mMap: GoogleMap? = null
    private var mSearchResult: SearchResult? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var mLocatorTask: LocatorTask? = null
    private var mTimer: Timer? = null
    private var mLastPositionTime: Long = 0
    private var mIsCameraMoving: Boolean = false
    private var mLastPosition: LatLng? = null
    private val mRealmManager = RealmManager(object : OnResultListener {
        override fun onWriteSuccessfully() {
            Log.i(TAG, "RealmManager-onWriteSuccessfully")
        }

        override fun onReadResult(data: List<PlaceRealm>) {
            if (BuildConfig.DEBUG) Log.i(TAG, "RealmManager-onReadResult: data=" + data.size)
            if (mSearchState == SearchState.CHECK_DATABASE) {
                runOnUiThread {
                    Log.i(TAG, "RealmManager-onReadResult: CHECK_DATABASE")
                    if (hasVisiblePlaces(data, mMap!!.projection.visibleRegion.latLngBounds)) {
                        Log.i(TAG, "RealmManager-onReadResult: hasVisiblePlaces")
                        data.forEach(Consumer { placeRealm: PlaceRealm? -> addMarker(mMap!!, placeRealm!!) })
                        searchFinished()
                    } else {
                        Log.i(TAG, "RealmManager-onReadResult: runSearch")
                        runSearch(DEFAULT_POI_ADDRESS)
                    }
                }
            }
        }
    })

    private var mSearchState = SearchState.NOT_IDEALIZED

    private enum class SearchState {
        NOT_IDEALIZED, WAITING_TO_RUN, CHECK_DATABASE, PROGRESS, COMPLETE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRealmManager.init(this)
        setContentView(R.layout.activity_maps)
        mSearchResult = SearchResult(findViewById(R.id.searchResult), object : CustomAdapter.OnItemClickListener {
            override fun onItemClick(item: PlaceRealm?) {
                mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(item!!.latLng, DEFAULT_ZOOM_FOCUS))
            }
        })
        findViewById<View>(R.id.search_there).setOnClickListener { v: View? -> runSearch(DEFAULT_POI_ADDRESS) }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        initArcGIS()
    }

    override fun onResume() {
        super.onResume()
        timerStart()
    }

    override fun onPause() {
        super.onPause()
        timerStop()
    }

    override fun onDestroy() {
        mRealmManager.close()
        super.onDestroy()
        timerStop()
    }

    //// Timer ////
    private fun timerStart() {
        if (mSearchState == SearchState.COMPLETE) return
        if (mTimer == null) mTimer = Timer()
        mTimer!!.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                timerTick()
            }
        }, TIME_NO_CAMERA_ACTION_FOR_SEARCH.toLong(), (1000 / TIMER_FPS).toLong())
    }

    private fun timerStop() {
        if (mTimer == null) return
        mTimer!!.cancel()
        mTimer = null;
    }

    private fun timerTick() {
        if (!mIsCameraMoving && mSearchState == SearchState.WAITING_TO_RUN && isLocationDataValid &&
                mLastPositionTime + TIME_NO_CAMERA_ACTION_FOR_SEARCH < System.currentTimeMillis()) {
            mSearchState = SearchState.CHECK_DATABASE
            mRealmManager.readData()
        }
    }

    private fun searchFinished() {
        mSearchState = SearchState.COMPLETE
        timerStop()
    }

    //// Location ////
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            initLocation()
        } else {
            mMap!!.isMyLocationEnabled = false
            mMap!!.uiSettings.isMyLocationButtonEnabled = false
        }
        mMap!!.setOnCameraMoveStartedListener { i: Int -> { mIsCameraMoving = true } }
        mMap!!.setOnMapClickListener { latLng: LatLng? -> googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM)) }
        mMap!!.setOnCameraIdleListener { if (mMap != null) onLocationChanged(mMap!!.cameraPosition.target, mMap!!.cameraPosition.zoom) }
    }

    private fun initLocation() {
        moveToDeviceLocation()
        mSearchState = SearchState.WAITING_TO_RUN
    }

    fun onLocationChanged(position: LatLng, zoom: Float) {
        Log.i(TAG, "onLocationChanged: position=$position, zoom=$zoom")
        mIsCameraMoving = false
        mLastPositionTime = System.currentTimeMillis()
        mLastPosition = position
    }

    private val isLocationDataValid: Boolean
        get() = mLastPositionTime != 0L && mLastPosition != null

    //convertFromLatLngPoint(mPosition);
    private val mapLocation: Point
        get() = Point(mLastPosition!!.longitude, mLastPosition!!.latitude, SpatialReferences.getWgs84())

    private fun moveToDeviceLocation() {
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //Adjust map parameters
            mMap!!.isMyLocationEnabled = true
            mMap!!.uiSettings.isMyLocationButtonEnabled = true


            //Start go to my location process
            fusedLocationClient!!.lastLocation.addOnSuccessListener(this) { location: Location? ->
                if (location != null) {
                    mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            LatLng(location.latitude, location.longitude), DEFAULT_ZOOM))
                }
            }
        } else {
            Log.e(TAG, "getDeviceLocation: Permission " + Manifest.permission.ACCESS_FINE_LOCATION + " not granted!")
        }
    }

    //// ArcGIS ////
    private fun initArcGIS() {
        // authentication with an API key or named user is required to access basemaps and other location services
        ArcGISRuntimeEnvironment.setApiKey(getString(R.string.arc_gis_key))

        // if permissions are not already granted, request permission from the user
        if (!(ContextCompat.checkSelfPermission(this, reqPermissions[0]) == PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(this, reqPermissions[1])
                        == PackageManager.PERMISSION_GRANTED)) {
            val requestCode = 2
            ActivityCompat.requestPermissions(this, reqPermissions, requestCode)
        }
        // create a LocatorTask from an online service
        mLocatorTask = LocatorTask(getString(R.string.world_geocode_service))
    }

    private fun runSearch(address: String) {
        val point = mapLocation
        Log.i(TAG, "runSearch: address=$address, point=$point")
        // check that address isn't null
        if (mMap != null && isLocationDataValid) {
            mSearchState = SearchState.PROGRESS

            // POI geocode parameters set from proximity SearchView or, if empty, device location
            val poiGeocodeParameters = GeocodeParameters()
            // get all attributes
            poiGeocodeParameters.resultAttributeNames.add("*")
            poiGeocodeParameters.preferredSearchLocation = point
            poiGeocodeParameters.searchArea = point
            poiGeocodeParameters.maxResults = MAX_RESULTS
            // Execute async task to find the address
            mLocatorTask!!.addDoneLoadingListener {
                Log.i(TAG, "runSearch: completed status=" + mLocatorTask!!.loadStatus)
                if (mLocatorTask!!.loadStatus == LoadStatus.LOADED) {
                    // Call geocodeAsync passing in an address
                    val geocodeResultListenableFuture = mLocatorTask!!
                            .geocodeAsync(address, poiGeocodeParameters)
                    geocodeResultListenableFuture.addDoneListener {
                        try {
                            // Get the results of the async operation
                            val geocodeResults = geocodeResultListenableFuture.get()
                            if (!geocodeResults.isEmpty()) {
                                runOnUiThread { displaySearchResult(geocodeResults) }
                            } else {
                                Toast.makeText(applicationContext, getString(R.string.location_not_found) + address,
                                        Toast.LENGTH_LONG).show()
                            }
                        } catch (e: InterruptedException) {
                            Log.e(TAG, "runSearch: Geocode error: " + e.message)
                            Toast.makeText(applicationContext, getString(R.string.geo_locate_error), Toast.LENGTH_LONG)
                                    .show()
                        } catch (e: ExecutionException) {
                            Log.e(TAG, "runSearch: Geocode error: " + e.message)
                            Toast.makeText(applicationContext, getString(R.string.geo_locate_error), Toast.LENGTH_LONG)
                                    .show()
                        }
                    }
                    searchFinished()
                } else {
                    Log.w(TAG, "runSearch: Trying to reload locator task")
                    mLocatorTask!!.retryLoadAsync()
                }
            }
            mLocatorTask!!.loadAsync()
        }
    }

    private fun displaySearchResult(geocodeResults: List<GeocodeResult>) {
        Log.i(TAG, "displaySearchResult: " + geocodeResults.size)
        mMap!!.clear()
        val resultList: MutableList<PlaceRealm> = ArrayList()
        val latLngBuilder = LatLngBounds.Builder()
        for (result in geocodeResults) {
            val resultPoint = result.displayLocation
            val point = LatLng(resultPoint.y, resultPoint.x)
            val resultItem = PlaceRealm(point, result.attributes)
            addMarker(mMap!!, resultItem)
            latLngBuilder.include(point)
            resultList.add(resultItem)
        }
        mSearchResult!!.setData(resultList)
        mRealmManager.writeData(resultList)
        mMap!!.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBuilder.build(), CAMERA_EDGE_PADDING_PX))
        searchFinished()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initLocation()
        } else {
            Toast.makeText(this, resources.getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }
}