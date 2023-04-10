package com.kickstarter.libs.featureflag

import android.app.Activity
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.kickstarter.libs.Build
import com.kickstarter.libs.Build.isInternal
import com.kickstarter.libs.featureflag.FeatureFlagClient.Companion.INTERNAL_INTERVAL
import com.kickstarter.libs.featureflag.FeatureFlagClient.Companion.RELEASE_INTERVAL
import timber.log.Timber

interface FeatureFlagClientType {

    /**
     * Will received a callback, that callback will usually
     * initialize the external library
     */
    fun initialize()

    /**
     * Will connect to the backend and fetch available values
     */
    fun fetch(context: Activity)

    /**
     * Will activate the latest values fetched from the backend
     */
    fun activate(context: Activity)

    /**
     * Will perform fetch and activate at the same time
     */
    fun fetchAndActivate(context: Activity)

    /**
     * Will return the active status for a boolean feature flag
     */
    fun getBoolean(FlagKey: FlagKey): Boolean

    /**
     * Will return the active value for a Double feature flag
     */
    fun getDouble(FlagKey: FlagKey): Double

    /**
     * Will return the active value for a Long feature flag
     */
    fun getLong(FlagKey: FlagKey): Long

    /**
     * Will return the active value for a String feature flag
     */
    fun getString(FlagKey: FlagKey): String
}

enum class FlagKey(val key: String) {
    ANDROID_FACEBOOK_LOGIN_REMOVE("android_facebook_login_remove"),
    ANDROID_HIDE_APP_RATING_DIALOG("android_hide_app_rating_dialog"),
    ANDROID_CONSENT_MANAGEMENT("android_consent_management"),
    ANDROID_CAPI_INTEGRATION("android_capi_integration"),
    ANDROID_GOOGLE_ANALYTICS("android_google_analytics"),
    ANDROID_PRE_LAUNCH_SCREEN("android_pre_launch_screen"),
}

fun FeatureFlagClient.getFetchInterval(): Long =
    if (this.build.isDebug || isInternal()) INTERNAL_INTERVAL
    else RELEASE_INTERVAL

class FeatureFlagClient(internal val build: Build, private val remoteConfig: FirebaseRemoteConfig?) : FeatureFlagClientType {

    override fun initialize() {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = getFetchInterval()
        }

        // - For the MVP no in-app defaults, will add them later on
        remoteConfig?.setConfigSettingsAsync(configSettings)

        log("${this.javaClass} initialized with interval: ${this.getFetchInterval()}")
    }

    override fun fetch(context: Activity) {
        remoteConfig?.fetch()
            ?.addOnCompleteListener(context) { task ->
                log("${this.javaClass} fetch completed: ${task.isSuccessful}")
            }
    }

    override fun activate(context: Activity) {
        remoteConfig?.activate()
            ?.addOnCompleteListener(context) { task ->
                log("${this.javaClass} activate completed: ${task.isSuccessful}")

                // Strategy loading 3 -> https://firebase.google.com/docs/remote-config/loading#strategy_3_load_new_values_for_next_startup
                if (task.isSuccessful && task.isComplete) {
                    fetch(context)
                }
            }
    }

    override fun fetchAndActivate(context: Activity) {
        remoteConfig?.fetchAndActivate()
            ?.addOnCompleteListener(context) { task ->
                log("${this.javaClass} fetchAndActivated completed: ${task.isSuccessful} ")
            }
    }

    override fun getBoolean(key: FlagKey): Boolean {
        val value = remoteConfig?.getBoolean(key.key) ?: false
        log("${this.javaClass} feature flag ${key.key}: $value")
        return value
    }

    override fun getDouble(key: FlagKey): Double {
        val value = remoteConfig?.getDouble(key.key) ?: 0.0
        log("${this.javaClass} feature flag ${key.key}: $value")
        return value
    }

    override fun getLong(key: FlagKey): Long {
        val value = remoteConfig?.getLong(key.key) ?: 0L
        log("${this.javaClass} feature flag ${key.key}: $value")
        return value
    }

    override fun getString(key: FlagKey): String {
        val value = remoteConfig?.getString(key.key) ?: ""
        log("${this.javaClass} feature flag ${key.key}: $value")
        return value
    }

    private fun log(message: String) {
        if (build.isDebug) Timber.d(message)
    }

    companion object {
        const val RELEASE_INTERVAL = 3600L
        const val INTERNAL_INTERVAL = 0L
    }
}
