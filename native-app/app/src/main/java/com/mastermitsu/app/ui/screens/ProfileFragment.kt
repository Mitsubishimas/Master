package com.mastermitsu.app.ui.screens

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.mastermitsu.app.R

class ProfileFragment : Fragment() {
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        val prefs = requireActivity().getSharedPreferences("master_prefs", android.content.Context.MODE_PRIVATE)
        
        view.findViewById<TextView>(R.id.tvProfileName).text = prefs.getString("saved_email", "Пользователь") ?: "Пользователь"
        view.findViewById<TextView>(R.id.tvProfileEmail).text = "mastermitsu.ru"
        
        view.findViewById<Button>(R.id.btnLogout).setOnClickListener {
            // Очищаем данные входа
            prefs.edit().clear().apply()
            
            // Открываем экран входа
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
        
        return view
    }
}
