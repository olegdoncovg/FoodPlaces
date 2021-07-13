package com.example.foodplaces.datamovel.arcgis

import android.content.Context
import android.util.Log
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment
import com.esri.arcgisruntime.loadable.LoadStatus
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult
import com.esri.arcgisruntime.tasks.geocode.LocatorTask
import com.example.foodplaces.R
import com.example.foodplaces.datamovel.DataSource
import com.example.foodplaces.datamovel.IResult
import com.example.foodplaces.datamovel.ISearch
import com.example.foodplaces.datamovel.realm.PlaceRealm
import com.example.foodplaces.viewmodel.IPlace
import com.google.android.gms.maps.model.LatLng
import java.util.*
import java.util.concurrent.ExecutionException

class ArcGISSearchManager : ISearch {
    companion object {
        private val TAG = ArcGISSearchManager::class.java.simpleName
        private const val MAX_RESULTS = 20
        private const val DEFAULT_POI_ADDRESS = "food"
    }

    private var mLocatorTask: LocatorTask? = null

    override fun init(context: Context) {
        // authentication with an API key or named user is required to access basemaps and other location services
        ArcGISRuntimeEnvironment.setApiKey(context.getString(R.string.arc_gis_key))

        // create a LocatorTask from an online service
        mLocatorTask = LocatorTask(context.getString(R.string.world_geocode_service))
    }

    override fun getData(latLong: LatLng, result: IResult) {
        val address: String = DEFAULT_POI_ADDRESS
        val point = ArcGISUtil.convertLatLngToPoint(latLong)
        Log.i(TAG, "runSearch: address=$address, point=$point")

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
                            result.onSuccess(formPlacesList(geocodeResults), DataSource.SERVER)
                        } else {
                            result.onFailed("runSearch: geocodeResults.isEmpty")
                        }
                    } catch (e: InterruptedException) {
                        result.onFailed("runSearch: Geocode error: " + e.message)
                    } catch (e: ExecutionException) {
                        result.onFailed("runSearch: Geocode error: " + e.message)
                    }
                }
            } else {
                Log.w(TAG, "runSearch: Trying to reload locator task")
                mLocatorTask!!.retryLoadAsync()
            }
        }
        mLocatorTask!!.loadAsync()
    }

    private fun formPlacesList(geocodeResults: List<GeocodeResult>): List<IPlace> {
        Log.i(TAG, "displaySearchResult: " + geocodeResults.size)
        val resultList: MutableList<PlaceRealm> = ArrayList()
        for (result in geocodeResults) {
            val point = ArcGISUtil.convertPointToLatLng(result.displayLocation)
            val resultItem = PlaceRealm(point, result.attributes)
            resultList.add(resultItem)
        }
        return resultList;
    }
}