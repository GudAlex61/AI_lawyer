package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.example.myapplication.auth.LoginActivity
import com.example.myapplication.auth.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var session: SessionManager // 🔐 Менеджер сессии

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔐 Инициализируем сессию и проверяем авторизацию
        session = SessionManager(this)
        if (!session.isLoggedIn()) {
            // Если не авторизован — редирект на логин
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish() // Закрываем MainActivity, чтобы нельзя было вернуться назад
            return
        }

        // ✅ Авторизован — продолжаем загрузку интерфейса
        setContentView(R.layout.activity_main)

        // 1. Get the WindowInsetsController
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // 2. Tell it that the appearance is light (so icons should be dark)
        windowInsetsController.isAppearanceLightStatusBars = true

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Устанавливаем начальный фрагмент И выделяем пункт чата
        if (savedInstanceState == null) {
            bottomNavigationView.selectedItemId = R.id.navigation_chat
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ChatFragment())
                .commit()
        }

        // Обработка нажатий на элементы навигации
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_documents -> {
                    replaceFragment(DocumentsFragment())
                    true
                }
                R.id.navigation_chat -> {
                    replaceFragment(ChatFragment())
                    true
                }
                R.id.navigation_profile -> {
                    replaceFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}