package com.example.smsservice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class StatusActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var statusIndicator: View
    private lateinit var statusText: TextView
    private lateinit var statusDescription: TextView
    private lateinit var deviceIdText: TextView
    private lateinit var lastConnectionText: TextView
    private lateinit var disconnectButton: MaterialButton

    companion object {
        private const val PREFS_NAME = "sms_service_prefs"
        private const val KEY_ACTIVATED = "activated"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_LAST_CONNECTION = "last_connection"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Check activation status
        if (!isActivated()) {
            navigateToActivation()
            return
        }

        setContentView(R.layout.activity_status)
        initViews()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        updateStatusUI()
    }

    private fun initViews() {
        statusIndicator = findViewById(R.id.statusIndicator)
        statusText = findViewById(R.id.statusText)
        statusDescription = findViewById(R.id.statusDescription)
        deviceIdText = findViewById(R.id.deviceIdText)
        lastConnectionText = findViewById(R.id.lastConnectionText)
        disconnectButton = findViewById(R.id.disconnectButton)
    }

    private fun setupListeners() {
        disconnectButton.setOnClickListener {
            showDisconnectConfirmation()
        }
    }

    private fun updateStatusUI() {
        val hasPermission = hasSmsPermission()

        if (hasPermission) {
            showActiveState()
        } else {
            showInactiveState()
        }

        // Update technical info
        updateTechnicalInfo()
    }

    private fun showActiveState() {
        statusIndicator.setBackgroundResource(R.drawable.status_indicator_active)
        statusText.text = getString(R.string.status_active)
        statusText.setTextColor(ContextCompat.getColor(this, R.color.status_active))
        statusDescription.text = getString(R.string.status_active_description)
    }

    private fun showInactiveState() {
        statusIndicator.setBackgroundResource(R.drawable.status_indicator_inactive)
        statusText.text = getString(R.string.status_inactive)
        statusText.setTextColor(ContextCompat.getColor(this, R.color.status_inactive))
        statusDescription.text = getString(R.string.status_inactive_description)

        // Add click listener to fix permissions
        statusDescription.setOnClickListener {
            navigateToPermissions()
        }
    }

    private fun updateTechnicalInfo() {
        val deviceId = prefs.getString(KEY_DEVICE_ID, "---") ?: "---"
        deviceIdText.text = getString(R.string.status_device_id, deviceId)

        val lastConnection = getLastConnectionString()
        lastConnectionText.text = getString(R.string.status_last_connection, lastConnection)
    }

    private fun getLastConnectionString(): String {
        // TODO: Implement actual last connection tracking
        val lastConnectionTime = prefs.getLong(KEY_LAST_CONNECTION, 0)
        
        return if (lastConnectionTime == 0L) {
            getString(R.string.status_last_connection_recent)
        } else {
            // TODO: Format timestamp properly
            getString(R.string.status_last_connection_recent)
        }
    }

    private fun showDisconnectConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.status_disconnect_confirm_title))
            .setMessage(getString(R.string.status_disconnect_confirm_message))
            .setPositiveButton(getString(R.string.dialog_yes)) { _, _ ->
                performDisconnect()
            }
            .setNegativeButton(getString(R.string.dialog_no)) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun performDisconnect() {
        // TODO: Send disconnect request to server

        sendDisconnectToServer { success ->
            if (success) {
                // Clear local state
                prefs.edit()
                    .putBoolean(KEY_ACTIVATED, false)
                    .remove(KEY_DEVICE_ID)
                    .remove(KEY_LAST_CONNECTION)
                    .apply()

                // Navigate back to activation
                navigateToActivation()
            } else {
                // TODO: Show error to user
            }
        }
    }

    private fun sendDisconnectToServer(callback: (Boolean) -> Unit) {
        // TODO: Implement actual server call
        // POST to server to deactivate this device

        // Simulating async call
        disconnectButton.postDelayed({
            callback(true)
        }, 500)
    }

    private fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isActivated(): Boolean {
        return prefs.getBoolean(KEY_ACTIVATED, false)
    }

    private fun navigateToActivation() {
        val intent = Intent(this, ActivationActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToPermissions() {
        val intent = Intent(this, PermissionExplanationActivity::class.java)
        startActivity(intent)
    }
}
