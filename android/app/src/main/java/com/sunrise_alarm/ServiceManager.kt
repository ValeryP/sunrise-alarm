package com.sunrise_alarm

import android.Manifest
import android.content.Intent
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatActivity.LOCATION_SERVICE
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

/*
 * @author Valeriy Palamarchuk
 * @email valeriij.palamarchuk@gmail.com
 * Created on 17.02.2020
 */
class ServiceManager {
    companion object {
        fun askPermissions(activity: AppCompatActivity) {
            Dexter.withActivity(activity)
                .withPermissions(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .withListener(
                    object : MultiplePermissionsListener {
                        override fun onPermissionsChecked(report: MultiplePermissionsReport) =
                            if (report.areAllPermissionsGranted()) {
                                checkGPSEnable(activity)
                            } else {
                                askPermissions(activity)
                            }

                        override fun onPermissionRationaleShouldBeShown(
                            permissions: List<PermissionRequest>, token: PermissionToken
                        ) = token.continuePermissionRequest()
                    })
                .check()
        }

        private fun checkGPSEnable(activity: AppCompatActivity) {
            val manager = activity.getSystemService(LOCATION_SERVICE) as LocationManager
            if (!manager.isProviderEnabled(GPS_PROVIDER)) {
                AlertDialog.Builder(activity).setMessage("Enable GPS to find closest bluetooth")
                    .setCancelable(false)
                    .setPositiveButton("Enable") { _, _ ->
                        activity.startActivity(Intent(ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                    .create()
                    .show()
            }
        }
    }

}