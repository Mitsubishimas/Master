package com.mastermitsu.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mastermitsu.app.ui.screens.*

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        
        if (savedInstanceState == null) {
            loadFragment(ForumFragment())
        }
        
        bottomNav.setOnItemSelectedListener { item ->
            loadFragment(when (item.itemId) {
                R.id.nav_forum -> ForumFragment()
                R.id.nav_chat -> ChatFragment()
                R.id.nav_news -> NewsFragment()
                R.id.nav_market -> MarketFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> ForumFragment()
            })
            true
        }
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
