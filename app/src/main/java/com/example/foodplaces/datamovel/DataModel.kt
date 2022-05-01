package com.example.foodplaces.datamovel

import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.example.foodplaces.BuildConfig
import com.example.foodplaces.datamovel.arcgis.ArcGISSearchManager
import com.example.foodplaces.datamovel.realm.PlaceRealm
import com.example.foodplaces.datamovel.realm.RealmManager
import com.example.foodplaces.viewmodel.IPlace
import com.google.android.gms.maps.model.LatLng

private val TAG = DataModel::class.java.simpleName

class DataModel : IDataModel {

    private var searchManager: ISearch = ArcGISSearchManager()
    private var searchResult: IResult = NoResult

    private val mRealmManager = RealmManager(object : RealmManager.OnResultListener {
        override fun onWriteSuccessfully() {
            Log.i(TAG, "RealmManager-onWriteSuccessfully")
        }

        override fun onReadResult(data: List<PlaceRealm>) {
            if (BuildConfig.DEBUG) Log.i(TAG, "RealmManager-onReadResult: data=" + data.size)
            searchResult.onSuccess(data, DataSource.LOCAL)
            searchResult = NoResult
        }
    })

    override fun init(activity: FragmentActivity) {
        searchManager.init(activity)
        mRealmManager.init(activity)
    }

    override fun getData(latLong: LatLng, force: Boolean, result: IResult) {
        Log.i(TAG, "getData: force=$force")
        if (force) {
            searchManager.getData(latLong, object : IResult {
                override fun onSuccess(places: List<IPlace>, dataSource: DataSource) {
                    mRealmManager.writeData(places)
                    result.onSuccess(places, dataSource)
                }

                override fun onFailed(errorMessage: String) {
                    result.onFailed(errorMessage)
                }
            })
        } else {
            searchResult = result;
            mRealmManager.readData();
        }
    }

    override fun close() {
        mRealmManager.close()
    }
}