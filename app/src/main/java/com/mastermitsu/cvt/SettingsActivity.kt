package com.mastermitsu.cvt

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var soundSwitch: Switch
    private lateinit var vibrationSwitch: Switch
    private lateinit var versionText: TextView
    private lateinit var btnCheckUpdate: Button
    
    private val prefs by lazy { getSharedPreferences("cvt_settings", MODE_PRIVATE) }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        soundSwitch = findViewById(R.id.soundSwitch)
        vibrationSwitch = findViewById(R.id.vibrationSwitch)
        versionText = findViewById(R.id.versionText)
        btnCheckUpdate = findViewById(R.id.btnCheckUpdate)
        
        loadSettings()
        setupListeners()
    }
    
    private fun loadSettings() {
        soundSwitch.isChecked = prefs.getBoolean("sound_enabled", true)
        vibrationSwitch.isChecked = prefs.getBoolean("vibration_enabled", true)
        
        // Получаем версию из BuildConfig
        val versionName = BuildConfig.VERSION_NAME
        versionText.text = "Версия: $versionName\nПакет: com.mastermitsu.cvt"
    }
    
    private fun setupListeners() {
        soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("sound_enabled", isChecked).apply()
        }
        vibrationSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("vibration_enabled", isChecked).apply()
        }
        btnCheckUpdate.setOnClickListener {
            UpdateChecker.checkForUpdate(this, true)
        }
    }
}
