package com.example.data.remote.pocketoption

import android.content.Context
import android.content.SharedPreferences
import com.example.core.security.KeyStoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PocketOptionSession(
    val ssid: String = "",
    val uid: String = "",
    val isDemo: Boolean = true,
    val isAuthenticated: Boolean = false
)

class SessionManager(
    context: Context,
    private val keyStoreManager: KeyStoreManager
) {
    companion object {
        private const val PREFS_NAME = "olro_pocket_option_session"
        private const val KEY_ENCRYPTED_SSID = "enc_ssid"
        private const val KEY_ENCRYPTED_UID = "enc_uid"
        private const val KEY_IS_DEMO = "is_demo"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _sessionState = MutableStateFlow(loadSession())
    val sessionState: StateFlow<PocketOptionSession> = _sessionState.asStateFlow()

    private fun loadSession(): PocketOptionSession {
        val encSsid = prefs.getString(KEY_ENCRYPTED_SSID, "") ?: ""
        val encUid = prefs.getString(KEY_ENCRYPTED_UID, "") ?: ""
        val isDemo = prefs.getBoolean(KEY_IS_DEMO, true)

        val decryptedSsid = if (encSsid.isNotEmpty()) keyStoreManager.decrypt(encSsid) else ""
        val decryptedUid = if (encUid.isNotEmpty()) keyStoreManager.decrypt(encUid) else ""

        return PocketOptionSession(
            ssid = decryptedSsid,
            uid = decryptedUid,
            isDemo = isDemo,
            isAuthenticated = decryptedSsid.isNotBlank()
        )
    }

    fun saveCredentials(ssid: String, uid: String, isDemo: Boolean) {
        val encSsid = if (ssid.isNotBlank()) keyStoreManager.encrypt(ssid) else ""
        val encUid = if (uid.isNotBlank()) keyStoreManager.encrypt(uid) else ""

        prefs.edit()
            .putString(KEY_ENCRYPTED_SSID, encSsid)
            .putString(KEY_ENCRYPTED_UID, encUid)
            .putBoolean(KEY_IS_DEMO, isDemo)
            .apply()

        _sessionState.value = PocketOptionSession(
            ssid = ssid,
            uid = uid,
            isDemo = isDemo,
            isAuthenticated = ssid.isNotBlank()
        )
    }

    fun setDemoMode(isDemo: Boolean) {
        prefs.edit().putBoolean(KEY_IS_DEMO, isDemo).apply()
        _sessionState.value = _sessionState.value.copy(isDemo = isDemo)
    }

    fun clearSession() {
        prefs.edit().clear().apply()
        _sessionState.value = PocketOptionSession()
    }
}
