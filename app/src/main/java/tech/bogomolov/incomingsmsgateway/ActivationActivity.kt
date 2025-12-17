package tech.bogomolov.incomingsmsgateway

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.widget.ProgressBar

class ActivationActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var activationCodeLayout: TextInputLayout
    private lateinit var activationCodeInput: TextInputEditText
    private lateinit var activateButton: MaterialButton
    private lateinit var progressBar: ProgressBar

    companion object {
        private const val PREFS_NAME = "sms_service_prefs"
        private const val KEY_ACTIVATED = "activated"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_USER_NAME = "user_name"
    }

    private val apiHelper = ActivationApiHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Check if already activated
        if (isActivated()) {
            navigateToNextScreen()
            return
        }

        setContentView(R.layout.activity_activation)
        initViews()
        setupListeners()
    }

    private fun initViews() {
        activationCodeLayout = findViewById(R.id.activationCodeLayout)
        activationCodeInput = findViewById(R.id.activationCodeInput)
        activateButton = findViewById(R.id.activateButton)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupListeners() {
        activateButton.setOnClickListener {
            attemptActivation()
        }

        activationCodeInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptActivation()
                true
            } else {
                false
            }
        }
    }

    private fun attemptActivation() {
        val code = activationCodeInput.text?.toString()?.trim() ?: ""

        // Clear previous errors
        activationCodeLayout.error = null

        // Validate input
        if (code.isEmpty()) {
            activationCodeLayout.error = getString(R.string.error_invalid_code)
            return
        }

        // Hide keyboard
        hideKeyboard()

        // Show loading state
        setLoadingState(true)

        // Simulate server call
        sendActivationToServer(code)
    }

    private fun sendActivationToServer(code: String) {
        apiHelper.activateDevice(code) { response ->
            if (response.success) {
                handleActivationResponse(
                    success = true,
                    deviceId = response.deviceId,
                    userName = response.userName,
                    errorType = null
                )
            } else {
                val errorType = when (response.error) {
                    ActivationApiHelper.ActivationError.INVALID_CODE -> ActivationError.INVALID_CODE
                    ActivationApiHelper.ActivationError.CODE_IN_USE -> ActivationError.CODE_IN_USE
                    ActivationApiHelper.ActivationError.SERVER_ERROR -> ActivationError.SERVER_ERROR
                    ActivationApiHelper.ActivationError.NETWORK_ERROR -> ActivationError.SERVER_ERROR
                    null -> ActivationError.SERVER_ERROR
                }
                handleActivationResponse(
                    success = false,
                    deviceId = null,
                    userName = null,
                    errorType = errorType
                )
            }
        }
    }

    private fun handleActivationResponse(
        success: Boolean,
        deviceId: String?,
        userName: String?,
        errorType: ActivationError?
    ) {
        setLoadingState(false)

        if (success && deviceId != null) {
            // Save activation state
            prefs.edit()
                .putBoolean(KEY_ACTIVATED, true)
                .putString(KEY_DEVICE_ID, deviceId)
                .putString(KEY_USER_NAME, userName ?: "משתמש")
                .apply()

            navigateToNextScreen()
        } else {
            // Show appropriate error
            val errorMessage = when (errorType) {
                ActivationError.INVALID_CODE -> getString(R.string.error_invalid_code)
                ActivationError.CODE_IN_USE -> getString(R.string.error_code_in_use)
                ActivationError.SERVER_ERROR -> getString(R.string.error_server_connection)
                null -> getString(R.string.error_server_connection)
            }
            activationCodeLayout.error = errorMessage
        }
    }

    private fun navigateToNextScreen() {
        val intent = Intent(this, PermissionExplanationActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun isActivated(): Boolean {
        return prefs.getBoolean(KEY_ACTIVATED, false)
    }

    private fun setLoadingState(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        activateButton.isEnabled = !loading
        activationCodeInput.isEnabled = !loading
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    enum class ActivationError {
        INVALID_CODE,
        CODE_IN_USE,
        SERVER_ERROR
    }
}
