package com.flover.googlemapapplicationfromscratch

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import java.io.IOException

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    //Widgets
    private lateinit var mSearchText: EditText

    // Variables
    private val errorDialogRequest: Int = 9001
    private var mLocationPermissionsGranted: Boolean = false
    private val locationPermissionRequestCode = 74

    private val defaultZoom: Float = 12f

    // These variables will be initialized later
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initializing widget
        mSearchText = findViewById(R.id.search_input)

        // Checking google map can be viewed or not
        if (isServiceOk()) {
            // Getting all permissions manually
            getLocationPermission()
            initSearch()
            // If all service are ok, we are listening to search button click
            findViewById<ImageView>(R.id.ic_done).setOnClickListener {
                geoLocate()

                // https://stackoverflow.com/questions/1109022/close-hide-the-android-soft-keyboard
                // When search done button clicked, input keyboard will be hidden
                hideKeyBord()
            }

            findViewById<ImageView>(R.id.position).setOnClickListener {
                getDeviceLocation()
                hideKeyBord()
            }
        }
    }

    private fun hideKeyBord() {
        val view = this.currentFocus
        view?.let {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun isServiceOk(): Boolean {
        var available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        return when {
            available == ConnectionResult.SUCCESS -> {
                true
            }
            GoogleApiAvailability.getInstance().isUserResolvableError(available) -> {
                var dialog: Dialog = GoogleApiAvailability.getInstance()
                    .getErrorDialog(this, available, errorDialogRequest)
                dialog.show()
                true
            }
            else -> {
                Toast.makeText(this, "You can't use this application!", Toast.LENGTH_SHORT).show()
                false
            }
        }
    }

    private fun getLocationPermission() {
        // Creating the array of permissions we need
        // https://stackoverflow.com/questions/31366229/how-to-initialize-an-array-in-kotlin-with-values/31366287
        var permission: Array<String> = arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )

        // Looping through the permissions to check everything
        // was ok
        permission.forEach {
            mLocationPermissionsGranted =
                ContextCompat.checkSelfPermission(
                    this.applicationContext,
                    it
                ) == PackageManager.PERMISSION_GRANTED
        }

        // If we need permission popup the dialog box to get permission
        if (!mLocationPermissionsGranted) {
            // The last parameter is a request code that can be any
            return ActivityCompat.requestPermissions(this, permission, 74)
        }
        return initMap()
    }

    // This will execute after the request dialog got user input
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        // We assuming the we didn't get permission yet
        mLocationPermissionsGranted = false
        // We are checking with our previous request code's result
        // and perform certain action according to the user input
        when (requestCode) {
            // We are checking the location permission's results
            locationPermissionRequestCode -> {
                // If the results are > 0 and everything was granted
                // we set mLocationPermissionsGranted to true other wise false

                if (grantResults.isNotEmpty()) {
                    grantResults.forEach {
                        mLocationPermissionsGranted =
                            it == PackageManager.PERMISSION_GRANTED
                    }
                    if (mLocationPermissionsGranted) {
                        initMap()
                    }
                }
            }
        }
    }

    // It will get the map asynchronously
    private fun initMap() {
        var mapFragment: SupportMapFragment =
            supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        // This will initialize the asynchronous task
        mapFragment.getMapAsync(this)
    }

    // Will execute when the map is ready and assign the map into
    // mMap variable
    override fun onMapReady(p0: GoogleMap?) {
        // https://stackoverflow.com/questions/34342413/what-is-the-kotlin-double-bang-operator
        // !! will throw NullPointer exception it p0 is null
        mMap = p0!!

        // if (mLocationPermissionsGranted){
        getDeviceLocation()

        // Setting the blue dot at device location
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = false
        }
        //}
    }

    private lateinit var latLng: LatLng

    // Getting the current location of the device
    private fun getDeviceLocation() {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        try {
            if (mLocationPermissionsGranted) {
                var location: Task<Location> = mFusedLocationProviderClient.lastLocation
                location.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        task.addOnSuccessListener { location ->
                            try {
                                latLng = LatLng(location.latitude, location.longitude)
                                moveCamera(latLng, defaultZoom)
                            } catch (e: IllegalStateException) {
                                Toast.makeText(
                                    this,
                                    "YOU HAVE REACHED MAX PLACE API REQUEST!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            // Added a red zone :) with circle option
                            /*mMap.addCircle(CircleOptions().center(latLng).radius(300.048)
                                .fillColor(Color.parseColor("#EC7063"))
                                .strokeWidth(1f).strokeColor(Color.parseColor("#85C1E9")))*/
                        }
                    }
                }
            }
        } catch (e: SecurityException) {

        }
    }

    private fun moveCamera(latLng: LatLng, zoom: Float) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
    }

    private fun initSearch() {
        /*var lambda : (TextView, Int, KeyEvent) -> Boolean = {textView, actionId, keyEvent ->
            doSomething()
        }*/

        // https://stackoverflow.com/questions/37201504/how-to-setoneditoractionlistener-with-kotlin
        mSearchText.setOnEditorActionListener { _, actionId, event ->
            if (actionId in intArrayOf(
                    EditorInfo.IME_ACTION_SEARCH,
                    EditorInfo.IME_ACTION_DONE
                ) || event.action in intArrayOf(
                    KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER
                )
            ) {
                // Execute geo search here
                geoLocate()
            }
            false
        }
    }

    private fun geoLocate() {
        var searchString: String = mSearchText.text.toString()
        var geoCoder = Geocoder(this)
        var list: List<Address> = ArrayList()
        try {
            list = geoCoder.getFromLocationName(searchString, 1)
        } catch (e: IOException) {

        }
        if (list.isNotEmpty()) {
            var address: Address = list[0]
            Log.i("ON_SUCCESS_ADDRESS", address.toString())
            // println(address.toString())
            var latLng = LatLng(address.latitude, address.longitude)
            moveCamera(latLng, defaultZoom)
            var options: MarkerOptions = MarkerOptions().position(latLng)
            mMap.addMarker(options)
            // Toast.makeText(this, address.toString(), Toast.LENGTH_LONG).show()
        }
    }
}
