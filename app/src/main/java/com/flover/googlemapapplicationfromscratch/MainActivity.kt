package com.flover.googlemapapplicationfromscratch

import android.app.Dialog
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment

class MainActivity : AppCompatActivity(), OnMapReadyCallback{

    private val errorDialogRequest: Int = 9001
    private var mLocationPermissionsGranted : Boolean = false
    private val locationPermissionRequestCode = 74

    private var mMap : GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (isServiceOk()){
            getLocationPermission()
        }
    }

    private fun isServiceOk() : Boolean{
        var available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        return when {
            available==ConnectionResult.SUCCESS -> {
                true
            }
            GoogleApiAvailability.getInstance().isUserResolvableError(available) -> {
                var dialog : Dialog = GoogleApiAvailability.getInstance().getErrorDialog(this, available, errorDialogRequest)
                dialog.show()
                true
            }
            else -> {
                Toast.makeText(this, "You can't use this application!", Toast.LENGTH_SHORT).show()
                false
            }
        }
    }

    private fun getLocationPermission(){
        // https://stackoverflow.com/questions/31366229/how-to-initialize-an-array-in-kotlin-with-values/31366287
        var permission : Array<String> = arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )

        permission.forEach {
            mLocationPermissionsGranted =
                ContextCompat.checkSelfPermission(this.applicationContext, it)==PackageManager.PERMISSION_GRANTED
        }

        if (!mLocationPermissionsGranted){
            ActivityCompat.requestPermissions(this, permission, 74)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        mLocationPermissionsGranted = false
        when(requestCode){
            locationPermissionRequestCode -> {
                if (grantResults.isNotEmpty()){
                    grantResults.forEach {
                        mLocationPermissionsGranted =
                            it == PackageManager.PERMISSION_GRANTED
                    }
                    if (mLocationPermissionsGranted){
                        initMap()
                    }
                }
            }
        }
    }

    private fun initMap(){
        var mapFragment : SupportMapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(p0: GoogleMap?) {
        mMap = p0
    }


}
