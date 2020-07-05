package xyz.dvnlabs.openmusix.util

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker


data class PermissionData(
    var permissionNeeded: String,
    var requestID: Int
)

data class PermissionGranted(
    var permission: String,
    var granted: Boolean
)

class PermissionHelper {
    companion object {
        fun checkPermission(
            context: Context,
            permissionNeeded: Array<PermissionData>
        ): List<PermissionGranted>? {
            val granted = ArrayList<PermissionGranted>()
            if (context !is Activity) return null

            for (permission in permissionNeeded) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        permission.permissionNeeded
                    ) == PermissionChecker.PERMISSION_GRANTED
                ) {
                    granted.add(PermissionGranted(permission.permissionNeeded, true))
                } else {
                    granted.add(PermissionGranted(permission.permissionNeeded, false))
                    ActivityCompat.requestPermissions(
                        context,
                        arrayOf(permission.permissionNeeded),
                        permission.requestID
                    )
                }
            }
            return granted
        }
    }
}