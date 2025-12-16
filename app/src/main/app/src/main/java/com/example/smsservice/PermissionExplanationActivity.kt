package com.example.smsservice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class PermissionExplanationActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var grantPermissionButton: MaterialButton
    private lateinit var laterButton: MaterialButton

    companion object {
        private const val PREFS_NAME = "sms_service_prefs"
        private const val KEY_ACTIVATED = "activated"
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            navigateToStatus()
        } else {
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Check if not activated - go back to activation
        if (!isActivated()) {
            navigateToActivation()
            return
        }

        // Check if permission already granted
        if (hasSmsPermission()) {
            navigateToStatus()
            return
        }

        setContentView(R.layout.activity_permission_explanation)
        initViews()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        // Check permission again when returning from settings
        if (hasSmsPermission()) {
            navigateToStatus()
        }
    }

    private fun initViews() {
        grantPermissionButton = findViewById(R.id.grantPermissionButton)
        laterButton = findViewById(R.id.laterButton)
    }

    private fun setupListeners() {
        grantPermissionButton.setOnClickListener {
            requestSmsPermission()
        }

        laterButton.setOnClickListener {
            // User chose to skip - go to status screen showing inactive state
            navigateToStatus()
        }
    }

    private fun requestSmsPermission() {
        when {
            hasSmsPermission() -> {
                navigateToStatus()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.RECEIVE_SMS) -> {
                // User previously denied - show explanation and request again
                requestPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.permission_denied_message))
            .setPositiveButton(getString(R.string.permission_open_settings)) { _, _ ->
                openAppSettings()
            }
            .setNegativeButton(getString(R.string.dialog_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
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

    private fun navigateToStatus() {
        val intent = Intent(this, StatusActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToActivation() {
        val intent = Intent(this, ActivationActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
