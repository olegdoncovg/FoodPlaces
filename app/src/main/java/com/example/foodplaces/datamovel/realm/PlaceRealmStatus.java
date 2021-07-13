package com.example.foodplaces.datamovel.realm;

public enum PlaceRealmStatus {
    Open("Open"),
    InProgress("In Progress"),
    Complete("Complete");
    String displayName;

    PlaceRealmStatus(String displayName) {
        this.displayName = displayName;
    }
}