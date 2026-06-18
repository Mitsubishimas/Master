package com.mastermitsu.cvt

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var soundSwitch: Switch
    private lateinit var vibrationSwitch: Switch
    private lateinit var darkThemeSwitch: Switch
    private lateinit var versionText: TextView
    private lateinit var soundSpinner: Spinner
    
    private val prefs by lazy { getSharedPreferences("cvt_settings", MODE_PRIVATE) }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        soundSwitch = findViewById(R.id.soundSwitch)
        vibrationSwitch = findViewById(R.id.vibrationSwitch)
        darkThemeSwitch = findViewById(R.id.darkThemeSwitch)
        versionText = findViewById(R.id.versionText)
        soundSpinner = findViewById(R.id.soundSpinner)
        
        loadSettings()
        setupListeners()
    }
    
    private fun loadSettings() {
        soundSwitch.isChecked = prefs.getBoolean("sound_enabled", true)
        vibrationSwitch.isChecked = prefs.getBoolean("vibration_enabled", true)
        darkThemeSwitch.isChecked = prefs.getBoolean("dark_theme", true)
        
        versionText.text = "Версия: 2.8.0\nПакет: com.mastermitsu.cvt"
        
        // Настройка звуков
        val sounds = arrayOf("По умолчанию", "Мелодия 1", "Мелодия 2", "Без звука")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sounds)
        soundSpinner.adapter = adapter
        soundSpinner.setSelection(prefs.getInt("sound_type", 0))
    }
    
    private fun setupListeners() {
        soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("sound_enabled", isChecked).apply()
            Toast.makeText(this, if (isChecked) "Звук включён" else "Звук выключен", Toast.LENGTH_SHORT).show()
        }
        
        vibrationSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("vibration_enabled", isChecked).apply()
            Toast.makeText(this, if (isChecked) "Вибрация включена" else "Вибрация выключена", Toast.LENGTH_SHORT).show()
        }
        
        darkThemeSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_theme", isChecked).apply()
            Toast.makeText(this, "Тема изменится при следующем запуске", Toast.LENGTH_SHORT).show()
        }
        
        soundSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                prefs.edit().putInt("sound_type", position).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
}
