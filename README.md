## FoodPlaces
There is release APK file: 
https://drive.google.com/file/d/1Uip7JQjzQEhGgdLDhrTC1w_JHrl4ludJ/view?usp=sharing

## Overview
Android application which show nearby food places as pins on the Google Maps. 
List of all found places also available for display. 
Tapping on a map pin or clicking list item show place details.

## How to compile a project
To compile project you need upddate values(FoodPlaces\app\src\main\res\values\strings.xml):

1) replace "google_maps_key" to your own created on google service for your project there
https://console.cloud.google.com/project/_/apiui/credential?hl=ru&_ga=2.177193185.518236667.1625941266-1066684077.1625722429

2) replace "arc_gis_key" to your own created on oficial site there
https://developers.arcgis.com/dashboard/

## Test task Description
You will be asked to develop and implement an Android application which will show nearby food places as pins on the Google Maps. List of all found places also should be available for display. Tapping on a map pin or clicking list item should show place details.

Technical requirements:
* Gradle must be used as a build tool
* you must use ArcGIS Runtime SDK for Android ( https://developers.arcgis.com/android/ ) as service that provides places
* limit a number of places fetched to 20. You must store them (last 20 fetched places) locally and use Realm ( https://realm.io/docs/java/latest/ ) for that
* on app start initial map camera position has to be centered to device location with zoom level 14. If there are stored places on app start which are visible on initial screen show them and don't make a service fetch call
* fetching places should happen only when all map interactions stopped (zoom, pan or camera movement). All fetched places have to be visible on map after search
* completed test assignment should be uploaded to GitHub and only link to it provided as a final solution. Project must have instructions for setting up environment and assembling APK"
XMA Header Image
ArcGIS Runtime API for Android | ArcGIS for Developers
developers.arcgis.com
