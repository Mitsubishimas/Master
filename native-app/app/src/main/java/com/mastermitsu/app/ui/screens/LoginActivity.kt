package com.mastermitsu.app.ui.screens

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.mastermitsu.app.MainActivity
import com.mastermitsu.app.R
import com.mastermitsu.app.data.api.ApiClient
import com.mastermitsu.app.data.models.LoginRequest
import kotlinx.coroutines.*

class LoginActivity : AppCompatActivity() {
    
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var prefs: SharedPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)
        errorText = findViewById(R.id.errorText)
        prefs = getSharedPreferences("master_prefs", MODE_PRIVATE)
        
        // Если уже вошли — проверяем токен
        val token = prefs.getString("user_token", null)
        if (!token.isNullOrEmpty()) {
            Log.d("Login", "Токен: $token")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        
        etEmail.setText(prefs.getString("saved_email", ""))
        btnLogin.setOnClickListener { login() }
    }
    
    private fun login() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        
        if (email.isEmpty() || password.isEmpty()) {
            errorText.visibility = TextView.VISIBLE
            errorText.text = "Введите email и пароль"
            return
        }
        
        btnLogin.isEnabled = false
        progressBar.visibility = ProgressBar.VISIBLE
        errorText.visibility = TextView.GONE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.instance.login(LoginRequest(email, password))
                Log.d("Login", "Ответ: success=${response.success}, token=${response.user?.token}")
                
                withContext(Dispatchers.Main) {
                    btnLogin.isEnabled = true
                    progressBar.visibility = ProgressBar.GONE
                    
                    if (response.success && response.user != null) {
                        val token = response.user.token ?: ""
                        // Сохраняем токен КАК ЕСТЬ с сервера
                        prefs.edit().apply {
                            putString("user_token", token)
                            putString("saved_email", email)
                            putInt("user_id", response.user.id)
                            putString("user_name", response.user.name)
                            apply()
                        }
                        
                        Log.d("Login", "Сохранён токен: $token")
                        
                        Toast.makeText(this@LoginActivity, "Вход выполнен", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        errorText.visibility = TextView.VISIBLE
                        errorText.text = response.error ?: "Неверный email или пароль"
                    }
                }
            } catch (e: Exception) {
                Log.e("Login", "Ошибка", e)
                withContext(Dispatchers.Main) {
                    btnLogin.isEnabled = true
                    progressBar.visibility = ProgressBar.GONE
                    errorText.visibility = TextView.VISIBLE
                    errorText.text = "Ошибка: ${e.message}"
                }
            }
        }
    }
}
