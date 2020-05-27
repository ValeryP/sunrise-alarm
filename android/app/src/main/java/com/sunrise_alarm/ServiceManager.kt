package com.sunrise_alarm

import android.Manifest.permission.*
import android.content.Intent
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatActivity.LOCATION_SERVICE
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.sunrise_alarm.Const.LOG_TAG

/*
 * @author Valeriy Palamarchuk
 * @email valeriij.palamarchuk@gmail.com
 * Created on 17.02.2020
 */
class ServiceManager {
    companion object {
        fun askPermissions(activity: AppCompatActivity, success: () -> Unit) {
            Dexter.withActivity(activity)
                .withPermissions(BLUETOOTH, BLUETOOTH_ADMIN, ACCESS_COARSE_LOCATION)
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                        Log.d(LOG_TAG, "onPermissionsChecked")
                        verifyPermissions(report, activity, success)
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: List<PermissionRequest>,
                        token: PermissionToken
                    ) {
                        Log.d(LOG_TAG, "onPermissionRationaleShouldBeShown")
                        token.continuePermissionRequest()
                    }
                })
                .check()
        }

        private fun verifyPermissions(
            report: MultiplePermissionsReport,
            activity: AppCompatActivity,
            success: () -> Unit
        ) {
            Log.d(
                LOG_TAG,
                "report.areAllPermissionsGranted(): ${report.areAllPermissionsGranted()}"
            )
            Log.d(
                LOG_TAG,
                "report.deniedPermissionResponses: ${report.deniedPermissionResponses.map { it.permissionName }}"
            )
            Log.d(
                LOG_TAG,
                "report.isAnyPermissionPermanentlyDenied: ${report.isAnyPermissionPermanentlyDenied}"
            )

            if (report.areAllPermissionsGranted()) {
                val manager = activity.getSystemService(LOCATION_SERVICE) as LocationManager
                Log.d(
                    LOG_TAG,
                    "manager.isProviderEnabled(GPS_PROVIDER): ${manager.isProviderEnabled(
                        GPS_PROVIDER
                    )}"
                )
                if (manager.isProviderEnabled(GPS_PROVIDER)) {
                    success()
                } else {
                    AlertDialog.Builder(activity)
                        .setMessage(R.string.enable_gps_message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.enable) { _, _ ->
                            activity.startActivity(Intent(ACTION_LOCATION_SOURCE_SETTINGS))
                        }
                        .create()
                        .show()
                }
            } else {
                askPermissions(activity, success)
            }
        }
    }

}