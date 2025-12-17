package tech.bogomolov.incomingsmsgateway

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class MainScreenActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var welcomeText: TextView
    private lateinit var statusText: TextView
    private lateinit var toggleButton: MaterialButton
    private lateinit var settingsButton: MaterialButton

    companion object {
        private const val PREFS_NAME = "sms_service_prefs"
        private const val KEY_ACTIVATED = "activated"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_SERVICE_PAUSED = "service_paused"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Check if not activated - go back to activation
        if (!isActivated()) {
            navigateToActivation()
            return
        }

        setContentView(R.layout.activity_main_screen)
        initViews()
        setupListeners()
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun initViews() {
        welcomeText = findViewById(R.id.welcomeText)
        statusText = findViewById(R.id.statusText)
        toggleButton = findViewById(R.id.toggleButton)
        settingsButton = findViewById(R.id.settingsButton)
    }

    private fun setupListeners() {
        toggleButton.setOnClickListener {
            toggleService()
        }

        settingsButton.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun updateUI() {
        // Update welcome text with user name
        val userName = prefs.getString(KEY_USER_NAME, "משתמש") ?: "משתמש"
        welcomeText.text = getString(R.string.main_welcome, userName)

        // Check if service can run (has permissions and not paused)
        val hasPermission = hasSmsPermission()
        val isPaused = isServicePaused()

        when {
            !hasPermission -> {
                // No permission
                statusText.text = getString(R.string.main_status_no_permission)
                statusText.setTextColor(ContextCompat.getColor(this, R.color.status_inactive))
                toggleButton.text = getString(R.string.main_button_grant_permission)
                toggleButton.icon = ContextCompat.getDrawable(this, R.drawable.ic_launcher_foreground)
            }
            isPaused -> {
                // Paused
                statusText.text = getString(R.string.main_status_paused)
                statusText.setTextColor(ContextCompat.getColor(this, R.color.status_inactive))
                toggleButton.text = getString(R.string.main_button_resume)
                toggleButton.setIconResource(android.R.drawable.ic_media_play)
            }
            else -> {
                // Active
                statusText.text = getString(R.string.main_status_active)
                statusText.setTextColor(ContextCompat.getColor(this, R.color.status_active))
                toggleButton.text = getString(R.string.main_button_pause)
                toggleButton.setIconResource(android.R.drawable.ic_media_pause)
            }
        }
    }

    private fun toggleService() {
        val hasPermission = hasSmsPermission()

        if (!hasPermission) {
            // Navigate to permission screen
            navigateToPermissions()
            return
        }

        val isPaused = isServicePaused()

        // Toggle the pause state
        prefs.edit()
            .putBoolean(KEY_SERVICE_PAUSED, !isPaused)
            .apply()

        // Update UI
        updateUI()

        // TODO: Start or stop the SmsReceiverService based on pause state
        if (!isPaused) {
            // Service was running, now pausing
            // Stop the service if needed
        } else {
            // Service was paused, now resuming
            // Start the service if needed
        }
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.main_settings_title))
            .setItems(arrayOf(
                getString(R.string.main_settings_permissions),
                getString(R.string.main_settings_disconnect)
            )) { _, which ->
                when (which) {
                    0 -> navigateToPermissions()
                    1 -> showDisconnectConfirmation()
                }
            }
            .setNegativeButton(getString(R.string.dialog_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showDisconnectConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.main_disconnect_confirm_title))
            .setMessage(getString(R.string.main_disconnect_confirm_message))
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
        // Clear activation state
        prefs.edit()
            .putBoolean(KEY_ACTIVATED, false)
            .remove(KEY_USER_NAME)
            .remove(KEY_SERVICE_PAUSED)
            .apply()

        // Navigate back to activation
        navigateToActivation()
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

    private fun isServicePaused(): Boolean {
        return prefs.getBoolean(KEY_SERVICE_PAUSED, false)
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
