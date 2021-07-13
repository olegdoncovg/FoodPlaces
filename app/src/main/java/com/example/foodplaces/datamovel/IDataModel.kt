package com.example.foodplaces.datamovel

import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.model.LatLng

interface IDataModel {
    companion object {
        fun getInstance(): IDataModel {
            return DataModel()
        }
    }

    fun init(activity: FragmentActivity)
    fun getData(latLong: LatLng, force: Boolean, result: IResult)
    fun close()
}