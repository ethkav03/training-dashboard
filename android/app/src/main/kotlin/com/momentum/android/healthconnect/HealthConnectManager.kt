package com.momentum.android.healthconnect

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord

private const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"
private const val HEALTH_CONNECT_PLAY_STORE_URL =
    "https://play.google.com/store/apps/details?id=$HEALTH_CONNECT_PACKAGE"

/**
 * Thin wrapper around the on-device Health Connect SDK: availability check,
 * the fixed permission set this app ever asks for, and the
 * ActivityResultContract used to request them. Steps is included in the
 * permission set (completes the flow the product spec asks for) but has no
 * home in our data model yet -- see docs/roadmap.md's "possible next steps".
 */
class HealthConnectManager(private val context: Context) {

    enum class Availability { AVAILABLE, NOT_INSTALLED, UPDATE_REQUIRED }

    val availability: Availability
        get() = when (HealthConnectClient.getSdkStatus(context, HEALTH_CONNECT_PACKAGE)) {
            HealthConnectClient.SDK_AVAILABLE -> Availability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Availability.UPDATE_REQUIRED
            else -> Availability.NOT_INSTALLED
        }

    // Lazy and only ever touched from suspend functions that are already
    // wrapped in runCatching (HealthConnectViewModel.syncNow) -- getOrCreate
    // throws if Health Connect isn't installed, and we don't want that to
    // happen just from constructing this class before the availability
    // check has had a chance to gate the UI.
    val client: HealthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
    )

    suspend fun hasAllPermissions(): Boolean =
        client.permissionController.getGrantedPermissions().containsAll(permissions)

    fun requestPermissionsContract(): ActivityResultContract<Set<String>, Set<String>> =
        PermissionController.createRequestPermissionResultContract()

    /** Deep-links to the Play Store listing when Health Connect isn't installed, or needs updating. */
    fun installOrUpdateIntent(): Intent = Intent(Intent.ACTION_VIEW, Uri.parse(HEALTH_CONNECT_PLAY_STORE_URL))
}
