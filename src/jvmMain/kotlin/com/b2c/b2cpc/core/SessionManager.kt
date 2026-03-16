package com.b2c.b2cpc.core

import java.util.prefs.Preferences

object SessionManager {
    private val prefs = Preferences.userRoot().node("com.b2c.b2cpc")

    var storeId: String
        get() = prefs.get("storeId", "")
        set(value) = prefs.put("storeId", value)
        
    var keyPath: String
        get() = prefs.get("keyPath", "")
        set(value) = prefs.put("keyPath", value)
        
    var windowWidth: Int
        get() = prefs.getInt("windowWidth", -1)
        set(value) = prefs.putInt("windowWidth", value)

    var windowHeight: Int
        get() = prefs.getInt("windowHeight", -1)
        set(value) = prefs.putInt("windowHeight", value)

    // Notification Settings
    var notifPosition: String
        get() = prefs.get("notifPosition", "우측 하단 (Windows 기본)")
        set(value) = prefs.put("notifPosition", value)

    var notifTitleSize: Int
        get() = prefs.getInt("notifTitleSize", 14)
        set(value) = prefs.putInt("notifTitleSize", value)

    var notifMessageSize: Int
        get() = prefs.getInt("notifMessageSize", 12)
        set(value) = prefs.putInt("notifMessageSize", value)

    var notifBgHex: String
        get() = prefs.get("notifBgHex", "FFFFFF")
        set(value) = prefs.put("notifBgHex", value)

    var notifTextHex: String
        get() = prefs.get("notifTextHex", "2C3E50")
        set(value) = prefs.put("notifTextHex", value)

    var notifDurationSec: Int
        get() = prefs.getInt("notifDurationSec", 5)
        set(value) = prefs.putInt("notifDurationSec", value)

    var notifSoundEnabled: Boolean
        get() = prefs.getBoolean("notifSoundEnabled", true)
        set(value) = prefs.putBoolean("notifSoundEnabled", value)

    var notifSoundPath: String
        get() = prefs.get("notifSoundPath", "")
        set(value) = prefs.put("notifSoundPath", value)

    var notifSoundStrategy: String
        get() = prefs.get("notifSoundStrategy", "개별 알림마다 재생")
        set(value) = prefs.put("notifSoundStrategy", value)

    var notifSoundVolume: Int
        get() = prefs.getInt("notifSoundVolume", 80)
        set(value) = prefs.putInt("notifSoundVolume", value)
        
    // Notification History for preventing duplicate alerts across restarts
    var notifiedHistoryDate: String
        get() = prefs.get("notifiedHistoryDate", "")
        set(value) = prefs.put("notifiedHistoryDate", value)

    var notifiedHistoryIds: String
        get() = prefs.get("notifiedHistoryIds", "")
        set(value) = prefs.put("notifiedHistoryIds", value)
        
    fun clearSession() {
        prefs.remove("storeId")
        prefs.remove("keyPath")
    }
}