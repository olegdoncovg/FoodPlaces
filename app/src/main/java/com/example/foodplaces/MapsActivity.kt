package com.example.foodplaces

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.foodplaces.datamovel.DataSource
import com.example.foodplaces.datamovel.IDataModel
import com.example.foodplaces.datamovel.IResult
import com.example.foodplaces.viewmodel.CustomAdapter
import com.example.foodplaces.viewmodel.IPlace
import com.example.foodplaces.viewmodel.MapManager
import com.example.foodplaces.viewmodel.SearchResultView
import com.google.android.gms.maps.model.LatLng

class MapsActivity : FragmentActivity(), MapManager.IMapInteraction {
    companion object {
        private val TAG = MapsActivity::class.java.simpleName
        private val reqPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION)
        private const val REQUEST_CODE: Int = 2
    }

    private val mDataModel = IDataModel.getInstance()
    private var mSearchResultView: SearchResultView? = null
    private var mMapManager: MapManager? = null

    private val mSearchResult: IResult = object : IResult {
        override fun onSuccess(places: List<IPlace>, dataSource: DataSource) {
            if (mMapManager != null && mSearchResultView != null)
                runOnUiThread {
                    mSearchResultView!!.showData(places)
                    mMapManager!!.onMapMarkersUpdate(places, true)
                }
        }

        override fun onFailed(errorMessage: String) {
            Log.e(TAG, "IResult::onFailed: error=$errorMessage")
            runOnUiThread {
                Toast.makeText(applicationContext, getString(R.string.geo_locate_error), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        mDataModel.init(this)
        mMapManager = MapManager(this)
        mSearchResultView = SearchResultView(findViewById(R.id.searchResult), object : CustomAdapter.OnItemClickListener {
            override fun onItemClick(item: IPlace) {
                if (mMapManager!!.isValid) mMapManager!!.animateToPlace(item)
            }
        })
        findViewById<View>(R.id.search_there).setOnClickListener(fun(v: View?) {
            if (mMapManager!!.isValid)
                mDataModel.getData(mMapManager!!.getLastPosition(), true, mSearchResult)
        })

        checkPermission()
    }

    override fun onResume() {
        super.onResume()
        if (mMapManager != null) mMapManager!!.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (mMapManager != null) mMapManager!!.onPause()
    }

    override fun onDestroy() {
        mDataModel.close()
        super.onDestroy()
        if (mMapManager != null) mMapManager!!.onDestroy()
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, reqPermissions[0]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, reqPermissions[1])
                == PackageManager.PERMISSION_GRANTED) {
            mMapManager!!.initLocation(true)
        } else {
            ActivityCompat.requestPermissions(this, reqPermissions, REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mMapManager!!.initLocation(true)
        } else {
            mMapManager!!.initLocation(false)
            Toast.makeText(this, resources.getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    override fun getActivity(): FragmentActivity {
        return this
    }

    override fun onMapCameraStop(currentLatLng: LatLng) {
        mDataModel.getData(currentLatLng, false, object : IResult {
            override fun onSuccess(places: List<IPlace>, dataSource: DataSource) {
                runOnUiThread {
                    if (dataSource == DataSource.LOCAL && mMapManager!!.hasVisiblePlaces(places)) {
                        mSearchResultView!!.showData(places)
                        mMapManager!!.onMapMarkersUpdate(places, false)
                    } else {
                        mDataModel.getData(currentLatLng, true, mSearchResult)
                    }
                }
            }

            override fun onFailed(errorMessage: String) {
                mSearchResult.onFailed(errorMessage)
            }
        })
    }
}