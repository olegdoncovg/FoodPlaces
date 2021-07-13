package com.example.foodplaces.viewmodel

import com.google.android.gms.maps.model.LatLng

interface IPlace {
    fun getLatLng(): LatLng
    fun getFullInfo(): String
    fun getShortInfo(): String
}