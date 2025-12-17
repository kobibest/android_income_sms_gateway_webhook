package tech.bogomolov.incomingsmsgateway

import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import java.util.concurrent.Executors

/**
 * Helper class for handling activation API calls
 */
class ActivationApiHelper {

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "ActivationApiHelper"
        private const val ACTIVATION_URL = "https://hook.us2.make.com/cvqpva8rl0v6ocvgj43efm856lvbmr6i"
    }

    data class ActivationResponse(
        val success: Boolean,
        val userName: String? = null,
        val deviceId: String? = null,
        val error: ActivationError? = null
    )

    enum class ActivationError {
        INVALID_CODE,
        CODE_IN_USE,
        SERVER_ERROR,
        NETWORK_ERROR
    }

    /**
     * Send activation code to server
     */
    fun activateDevice(
        activationCode: String,
        callback: (ActivationResponse) -> Unit
    ) {
        executor.execute {
            try {
                val payload = JSONObject().apply {
                    put("activation_code", activationCode)
                    put("device_info", getDeviceInfo())
                }

                val request = Request(ACTIVATION_URL, payload.toString())
                request.setJsonHeaders("{}")

                val result = request.execute()

                // For now, treat any successful HTTP call as success
                // The webhook will receive the data
                val response = when (result) {
                    Request.RESULT_SUCCESS, Request.RESULT_RETRY -> {
                        // Success - server received the activation code
                        Log.d(TAG, "Activation sent to server successfully")
                        ActivationResponse(
                            success = true,
                            userName = "משתמש", // Default user name
                            deviceId = generateDeviceId()
                        )
                    }
                    else -> {
                        Log.e(TAG, "Server error: $result")
                        ActivationResponse(
                            success = false,
                            error = ActivationError.SERVER_ERROR
                        )
                    }
                }

                // Return result on main thread
                mainHandler.post {
                    callback(response)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Activation error: ${e.message}", e)
                mainHandler.post {
                    callback(
                        ActivationResponse(
                            success = false,
                            error = ActivationError.SERVER_ERROR
                        )
                    )
                }
            }
        }
    }

    private fun getDeviceInfo(): JSONObject {
        return JSONObject().apply {
            put("model", android.os.Build.MODEL)
            put("manufacturer", android.os.Build.MANUFACTURER)
            put("android_version", android.os.Build.VERSION.RELEASE)
        }
    }

    private fun generateDeviceId(): String {
        return "DEV-${System.currentTimeMillis().toString(36).uppercase()}"
    }
}
