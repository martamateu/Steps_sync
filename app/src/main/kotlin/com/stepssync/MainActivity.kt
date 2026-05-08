package com.stepssync

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Minimal launcher activity whose sole purpose is to request Health Connect
 * permissions.  Once permissions are granted the user can close the app –
 * [SyncWorker] runs silently in the background via WorkManager.
 *
 * This activity also hosts the
 * `androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE` intent-filter required
 * by Health Connect so the system can display the rationale when needed.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"

        private val PERMISSIONS = setOf(
            HealthPermission.getReadPermission(StepsRecord::class)
        )
    }

    // Health Connect permission launcher
    private val requestPermissionsLauncher =
        registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { grantedPermissions ->
            updateUi(grantedPermissions.containsAll(PERMISSIONS))
        }

    private lateinit var statusText: TextView
    private lateinit var grantButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Build a minimal UI programmatically – no XML layout needed
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
        }

        statusText = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            text = getString(R.string.status_running)
        }

        grantButton = Button(this).apply {
            text = getString(R.string.btn_grant_permissions)
            setOnClickListener {
                requestPermissionsLauncher.launch(PERMISSIONS)
            }
        }

        layout.addView(statusText)
        layout.addView(grantButton)
        setContentView(layout)

        // Check current permission state and update UI accordingly
        lifecycleScope.launch {
            checkAndUpdatePermissions()
        }
    }

    private suspend fun checkAndUpdatePermissions() {
        try {
            val repository = HealthConnectRepository(this)
            val hasPermissions = repository.hasPermissions()
            updateUi(hasPermissions)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check Health Connect permissions", e)
        }
    }

    private fun updateUi(hasPermissions: Boolean) {
        if (hasPermissions) {
            statusText.text = getString(R.string.status_permissions_granted)
            grantButton.isEnabled = false
        } else {
            statusText.text = getString(R.string.status_permissions_missing)
            grantButton.isEnabled = true
        }
    }
}
