package com.sui.nothingunhidenotif

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private val prefName = "notif_prefs"
    private val cfg = UiCfg()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadPrefsIntoCfg()

        val root = ScrollView(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        root.addView(box)

        // bar
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, sysBars.top, v.paddingRight, v.paddingBottom)
            insets
        }
        
        val status = TextView(this).apply { textSize = 14f }

        fun addTitle(text: String) {
            box.addView(TextView(this).apply { this.text = text; textSize = 18f })
        }

        fun addSwitch(label: String, initial: Boolean, onChange: (Boolean) -> Unit) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val tv = TextView(this).apply { text = label; textSize = 16f }
            val sw = Switch(this).apply {
                isChecked = initial
                setOnCheckedChangeListener { _, v ->
                    onChange(v)
                    saveCfgToPrefs()
                    status.text = "Saved"
                }
            }
            row.addView(tv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(sw)
            box.addView(row)
        }

        addTitle("NothingOS Unhide Notification")

        addSwitch("Unhide notification content of locked apps", cfg.enabled) { cfg.enabled = it }
        
        addSwitch("Unhide on lockscreen", cfg.onLockscreen) { cfg.onLockscreen = it }

        val btnRestart = Button(this).apply {
            text = "Restart SystemUI"
            setOnClickListener {
                status.text = "Restarting SystemUI..."
                Thread {
                    val ok = restartSystemUI()
                    runOnUiThread {
                        status.text = if (ok) "SystemUI restarted" else "Failed (su?)"
                    }
                }.start()
            }
        }
        box.addView(btnRestart)
        //box.addView(status)
        setContentView(root)
    }

    // prefs
    private fun sp(): SharedPreferences =
        getSharedPreferences(prefName, MODE_WORLD_READABLE)

    private fun loadPrefsIntoCfg() {
        val sp = sp()
        cfg.enabled = sp.getBoolean("enabled", true)
        cfg.onLockscreen = sp.getBoolean("onLockscreen", false)
    }

    private fun saveCfgToPrefs() {
        sp().edit()
            .putBoolean("enabled", cfg.enabled)
            .putBoolean("onLockscreen", cfg.onLockscreen)
            .commit()
    }

    // utils
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()
    
    private fun su(cmd: String): Pair<Int, String> {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            val out = p.inputStream.bufferedReader().readText()
            val err = p.errorStream.bufferedReader().readText()
            val code = p.waitFor()
            code to (out + if (err.isNotBlank()) "\n$err" else "")
        } catch (t: Throwable) {
            -1 to (t.javaClass.simpleName + ": " + (t.message ?: ""))
        }
    }
    
    private fun restartSystemUI(): Boolean {
        val cmds = listOf(
            "pkill -f com.android.systemui",
            "killall com.android.systemui",
            "am crash com.android.systemui"
        )
        for (c in cmds) {
            val (code, _) = su(c)
            if (code == 0) return true
        }
        return false
    }

    private class UiCfg {
        var enabled: Boolean = true
        var onLockscreen: Boolean = false
    }
}
