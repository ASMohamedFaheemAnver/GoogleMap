package com.flover.googlemapapplicationfromscratch

import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

class MainActivity : AppCompatActivity() {

    private val errorDialogRequest: Int = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        isServiceOk()
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
                Toast.makeText(this, "You can use this application!", Toast.LENGTH_SHORT).show()
                false
            }
        }
    }
}
