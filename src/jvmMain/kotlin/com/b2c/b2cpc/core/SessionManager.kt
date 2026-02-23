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
        
    fun clearSession() {
        prefs.remove("storeId")
        prefs.remove("keyPath")
    }
}
