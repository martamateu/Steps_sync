package com.stepssync.app

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.stepssync.R
import com.stepssync.data.HealthConnectRepository
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val repository by lazy { HealthConnectRepository(this) }
    private val activityScope = MainScope()

    private val requestPermissionsLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        renderPermissions(grantedPermissions.containsAll(repository.requiredPermissions))
    }

    private lateinit var statusText: TextView
    private lateinit var grantButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createContentView())

        activityScope.launch {
            refreshState()
        }
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }

    private suspend fun refreshState() {
        try {
            when (repository.sdkStatus()) {
                HealthConnectClient.SDK_AVAILABLE -> {
                    renderPermissions(repository.hasPermissions())
                }
                else -> {
                    statusText.text = getString(R.string.status_health_connect_unavailable)
                    grantButton.isEnabled = false
                }
            }
        } catch (error: Exception) {
            Log.e(TAG, "Failed to load permission state", error)
            statusText.text = getString(R.string.status_health_connect_unavailable)
            grantButton.isEnabled = false
        }
    }

    private fun renderPermissions(hasPermissions: Boolean) {
        statusText.text = if (hasPermissions) {
            getString(R.string.status_permissions_granted)
        } else {
            getString(R.string.status_permissions_missing)
        }
        grantButton.isEnabled = !hasPermissions
    }

    private fun createContentView(): LinearLayout {
        statusText = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            gravity = Gravity.CENTER
            text = getString(R.string.status_running)
            textSize = 16f
        }

        grantButton = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            text = getString(R.string.btn_grant_permissions)
            setOnClickListener {
                requestPermissionsLauncher.launch(repository.requiredPermissions)
            }
        }

        return LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            addView(statusText)
            addView(grantButton)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
