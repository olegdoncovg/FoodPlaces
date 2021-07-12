package com.example.foodplaces

import com.example.foodplaces.realm.PlaceRealm
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions

class MapUtil {
    companion object {
        @JvmStatic
        fun hasVisiblePlaces(data: List<PlaceRealm>, latLngBounds: LatLngBounds): Boolean {
            for (placeRealm in data) {
                if (latLngBounds.contains(placeRealm.latLng))
                    return true
            }
            return false
        }

        @JvmStatic
        fun addMarker(googleMap: GoogleMap, resultItem: PlaceRealm) {
            googleMap.addMarker(MarkerOptions().position(resultItem.latLng).title(resultItem.shortInfo))
        }
    }
}