package com.example.foodplaces.datamovel.realm;

import androidx.annotation.NonNull;

import com.example.foodplaces.viewmodel.IPlace;
import com.google.android.gms.maps.model.LatLng;

import java.util.Map;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class PlaceRealm extends RealmObject implements IPlace {
    @PrimaryKey
    private String id;
    @Required
    private String status = PlaceRealmStatus.Open.name();

    private double latitude;
    private double longitude;
    private String fullInfo;
    private String shortInfo;

    public PlaceRealm(LatLng latLng, Map<String, Object> attributes) {
        this.latitude = latLng.latitude;
        this.longitude = latLng.longitude;
        this.id = getId(latitude, longitude);
        fullInfo = attributes.get("LongLabel").toString();
        shortInfo = attributes.get("ShortLabel").toString();
    }

    public PlaceRealm() {
    }

    public PlaceRealm(IPlace place) {
        this.latitude = place.getLatLng().latitude;
        this.longitude = place.getLatLng().longitude;
        this.id = getId(latitude, longitude);
        fullInfo = place.getFullInfo();
        shortInfo = place.getShortInfo();
    }

    @NonNull
    private static String getId(double latitude, double longitude) {
        return latitude + "_" + longitude;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(PlaceRealmStatus status) {
        this.status = status.name();
    }

    public LatLng getLatLng() {
        return new LatLng(latitude, longitude);
    }

    public String getFullInfo() {
        return fullInfo;
    }

    public void setFullInfo(String fullInfo) {
        this.fullInfo = fullInfo;
    }

    public String getShortInfo() {
        return shortInfo;
    }

    public void setShortInfo(String shortInfo) {
        this.shortInfo = shortInfo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @NonNull
    @Override
    public String toString() {
        return "PlaceRealm{" +
                "id=" + id +
                ", status='" + status +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
//                ", fullInfo='" + fullInfo +
                ", shortInfo='" + shortInfo +
                '}';
    }
}