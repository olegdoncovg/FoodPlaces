package com.example.foodplaces.datamovel.arcgis

import com.esri.arcgisruntime.geometry.Point
import com.google.android.gms.maps.model.LatLng

class ArcGISUtil {
    companion object {
        fun convertLatLngToPoint(latLang: LatLng): Point {
            return Point(latLang.longitude, latLang.latitude)
        }

        fun convertPointToLatLng(point: Point): LatLng {
            return LatLng(point.y, point.x)
        }
    }
}