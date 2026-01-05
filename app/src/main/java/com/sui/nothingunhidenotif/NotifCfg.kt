package com.sui.nothingunhidenotif

import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge

object NotifCfg {
    private const val MOD_PKG = "com.sui.nothingunhidenotif"
    private const val PREF_NAME = "notif_prefs"
    private fun log(s: String) = XposedBridge.log("NothingUnhideNotif: $s")

    data class Cfg(
        val enabled: Boolean = true,
        val onLockscreen: Boolean = false
    )

    private val xsp: XSharedPreferences by lazy { XSharedPreferences(MOD_PKG, PREF_NAME) }

    @Volatile private var cached = Cfg()
    @Volatile private var inited = false

    fun initOnce() {
        if (inited) return
        synchronized(this) {
            if (inited) return
            try {
                log("reload(initOnce)")
                xsp.reload()
            } catch (t: Throwable) {
                log("reload(initOnce) FAIL: ${t.javaClass.simpleName}:${t.message}")
            }
            cached = readNoReload()
            inited = true
        }
    }

    fun get(): Cfg = cached

    fun onLockscreenCached(): Boolean = cached.onLockscreen

    private fun readNoReload(): Cfg {
        return Cfg(
            enabled = xsp.getBoolean("enabled", true),
            onLockscreen = xsp.getBoolean("onLockscreen", false)
        )
    }
}