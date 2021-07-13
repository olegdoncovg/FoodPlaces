package com.example.foodplaces.datamovel

import android.content.Context
import com.google.android.gms.maps.model.LatLng

interface ISearch {
    fun init(context: Context)
    fun getData(latLong: LatLng, result: IResult)
}