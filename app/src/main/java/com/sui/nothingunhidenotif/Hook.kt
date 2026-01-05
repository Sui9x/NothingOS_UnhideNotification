package com.sui.nothingunhidenotif

import android.content.res.Resources
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.widget.ImageView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class Hook : IXposedHookLoadPackage {

    private fun log(msg: String) = XposedBridge.log("NothingUnhideNotif: $msg")

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.android.systemui") return
        log("handleLoadPackage: ${lpparam.packageName}")
        val cl = lpparam.classLoader
        
        NotifCfg.initOnce()

        val cfg = NotifCfg.get()
        
        if (cfg.enabled) {
            hookIsAppLocked(cl)
            log("unhide hook enabled")
            if (cfg.onLockscreen) {
                hookSetSensitive(cl)
                log("onlockscreen enabled")
            } else {
                log("onlockscreen disabled")
            }
            log("hooks installed for SystemUI (cl=$cl)")
        } else {
            log("unhide hook disabled")
        }
    }
    
    // hookAll overloads
    private fun hookAll(className: String, methodName: String, hook: XC_MethodHook) {
        try {
            val c = XposedHelpers.findClass(className, null)
            XposedBridge.hookAllMethods(c, methodName, hook)
            log("HOOKED(all): $className->$methodName (boot)")
        } catch (t: Throwable) {
            log("FAILED: $className->$methodName (boot) : ${t.javaClass.simpleName}: ${t.message}")
        }
    }

    private fun hookAll(cl: ClassLoader, className: String, methodName: String, hook: XC_MethodHook) {
        try {
            val c = XposedHelpers.findClass(className, cl)
            XposedBridge.hookAllMethods(c, methodName, hook)
            log("HOOKED(all): $className->$methodName (cl)")
        } catch (t: Throwable) {
            log("FAILED: $className->$methodName (cl) : ${t.javaClass.simpleName}: ${t.message}")
        }
    }
    
    private fun hookIsAppLocked(cl: ClassLoader) {
        // Hook: com.android.systemui.statusbar.notification.row.ExpandableNotificationRow.isAppLocked()
        // isAppLockedメソッドをフックしAppLockerによる通知内容非表示を無効化しますが、ロック画面ではsetSensitiveメソッドによって通知内容が非表示になります。
        // Hook the isAppLocked method to disable the hiding of notification content by AppLocker, but on the lock screen, the notification content is hidden by the setSensitive method.
        hookAll(
            cl,
            "com.android.systemui.statusbar.notification.row.ExpandableNotificationRow",
            "isAppLocked",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = false
                    //log("CALLED: ExpandableNotificationRow->isAppLocked : forced FALSE")
                }
            }
        )
    }

    private fun hookSetSensitive(cl: ClassLoader) {
        // Hook: com.android.systemui.statusbar.notification.collection.NotificationEntry.setSensitive()
        // setSensitiveメソッドをフックしロック画面での通知内容非表示も無効化しますが、これによりデバイスのアプリ通知設定に従った通知内容非表示も無効になります。
        // Hook the setSensitive method to disables the hiding of notification content also on the lock screen, but this also disables hiding notification content according to the device's app notification settings.
        hookAll(
            cl,
            "com.android.systemui.statusbar.notification.collection.NotificationEntry",
            "setSensitive",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    // (boolean, boolean)
                    try {
                        if (param.args.size >= 2) {
                            param.args[0] = false
                            param.args[1] = false
                        } else if (param.args.isNotEmpty()) {
                            param.args[0] = false
                        }
                    } catch (_: Throwable) {}
                    //log("CALLED: NotificationEntry->setSensitive(..) : forced FALSE")
                }
            }
        )
    }
}
