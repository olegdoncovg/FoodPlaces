package com.example.foodplaces.viewmodel

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.foodplaces.BuildConfig
import com.example.foodplaces.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import java.util.*

class MapManager(mapInteraction: IMapInteraction) : OnMapReadyCallback {
    companion object {
        private val TAG = MapManager::class.java.simpleName
        private val NO_LOCATION = LatLng(.0, .0)
        private const val CAMERA_EDGE_PADDING_PX = 100
        private const val DEFAULT_ZOOM = 14f
        private const val DEFAULT_ZOOM_FOCUS = 17f
        private const val TIME_NO_CAMERA_ACTION_FOR_SEARCH = 1000
        private const val TIMER_FPS = 40
    }

    private var mInteraction: IMapInteraction = mapInteraction

    private var mMap: GoogleMap? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null

    private var mLastPositionTime: Long = 0
    private var mLastPosition: LatLng = NO_LOCATION
    private var mTimer: Timer? = null

    private var mSearchState = SearchState.NOT_IDEALIZED
    private enum class SearchState {
        NOT_IDEALIZED, WAITING_TO_RUN, PROGRESS, COMPLETE
    }

    init {
        val activity = mInteraction.getActivity()
        val mapFragment = activity.supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
    }

    fun onMapMarkersUpdate(places: List<IPlace>, focusOnPlaces: Boolean) {
        mMap!!.clear()
        val latLngBuilder = LatLngBounds.Builder()
        for (place in places) {
            addMarker(place)
            latLngBuilder.include(place.getLatLng())
        }

        if (focusOnPlaces)
            mMap!!.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBuilder.build(), CAMERA_EDGE_PADDING_PX))
        searchFinished()
    }

    //// Location ////
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (ContextCompat.checkSelfPermission(mInteraction.getActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            initLocation(true)
        } else {
            mMap!!.isMyLocationEnabled = false
            mMap!!.uiSettings.isMyLocationButtonEnabled = false
        }
        mMap!!.setOnMapClickListener { latLng: LatLng ->
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM))
        }
        mMap!!.setOnCameraMoveListener {
            if (mMap != null) onLocationChanged(mMap!!.cameraPosition.target)
        }
    }

    fun initLocation(locationPermitted: Boolean) {
        if (mMap == null) return
        if (locationPermitted) moveToDeviceLocation()
        waitForMapCameraStop()
    }

    val isValid: Boolean
        get() = mMap != null && mLastPosition != NO_LOCATION

    fun hasVisiblePlaces(data: List<IPlace>): Boolean {
        if (!isValid) return false
        val latLngBounds: LatLngBounds = mMap!!.projection.visibleRegion.latLngBounds
        for (placeRealm in data) {
            if (latLngBounds.contains(placeRealm.getLatLng()))
                return true
        }
        return false
    }

    fun animateToPlace(item: IPlace) {
        mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(item.getLatLng(), DEFAULT_ZOOM_FOCUS))
    }

    fun getLastPosition(): LatLng {
        return mLastPosition
    }

    private fun waitForMapCameraStop() {
        timerStart()
        mSearchState = SearchState.WAITING_TO_RUN
    }

    private fun onLocationChanged(position: LatLng) {
        Log.i(TAG, "onLocationChanged: position=$position")
        mLastPositionTime = System.currentTimeMillis()
        mLastPosition = position
    }

    private fun moveToDeviceLocation() {
        if (BuildConfig.DEBUG) Objects.requireNonNull(mMap)
        if (ContextCompat.checkSelfPermission(mInteraction.getActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //Adjust map parameters
            mMap!!.isMyLocationEnabled = true
            mMap!!.uiSettings.isMyLocationButtonEnabled = true


            //Start go to my location process
            fusedLocationClient!!.lastLocation.addOnSuccessListener(
                    mInteraction.getActivity()) { location: Location? ->
                if (location != null) {
                    mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            LatLng(location.latitude, location.longitude), DEFAULT_ZOOM))
                }
            }
        } else {
            Log.e(TAG, "getDeviceLocation: Permission " + Manifest.permission.ACCESS_FINE_LOCATION + " not granted!")
        }
    }

    private fun addMarker(resultItem: IPlace) {
        mMap!!.addMarker(MarkerOptions().position(resultItem.getLatLng()).title(resultItem.getShortInfo()))
    }

    //// Timer ////
    fun onResume() {
        timerStart()
    }

    fun onPause() {
        timerStop()
    }

    fun onDestroy() {
        timerStop()
    }

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
        mTimer = null
    }

    private fun timerTick() {
        if (isValid && mSearchState == SearchState.WAITING_TO_RUN &&
                mLastPositionTime + TIME_NO_CAMERA_ACTION_FOR_SEARCH < System.currentTimeMillis()) {
            mSearchState = SearchState.PROGRESS
            mInteraction.onMapCameraStop(getLastPosition())
        }
    }

    private fun searchFinished() {
        mSearchState = SearchState.COMPLETE
        timerStop()
    }

    interface IMapInteraction {
        fun getActivity(): FragmentActivity
        fun onMapCameraStop(currentLatLng: LatLng)
    }
}
