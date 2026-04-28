package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private var chatFragment: ChatFragment? = null
    private var documentsFragment: DocumentsFragment? = null
    private var profileFragment: ProfileFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            chatFragment = ChatFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, chatFragment!!, "chat")
                .commit()
            bottomNavigationView.selectedItemId = R.id.navigation_chat
        } else {
            // Восстанавливаем ссылки на уже добавленные фрагменты после пересоздания Activity
            chatFragment = supportFragmentManager.findFragmentByTag("chat") as? ChatFragment
            documentsFragment = supportFragmentManager.findFragmentByTag("documents") as? DocumentsFragment
            profileFragment = supportFragmentManager.findFragmentByTag("profile") as? ProfileFragment
        }

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_documents -> {
                    showFragment("documents")
                    true
                }
                R.id.navigation_chat -> {
                    showFragment("chat")
                    true
                }
                R.id.navigation_profile -> {
                    showFragment("profile")
                    true
                }
                else -> false
            }
        }
    }

    private fun showFragment(tag: String) {
        val transaction = supportFragmentManager.beginTransaction()

        // Скрываем все текущие видимые фрагменты
        supportFragmentManager.fragments.forEach { fragment ->
            if (!fragment.isHidden) {
                transaction.hide(fragment)
            }
        }

        // Показываем или добавляем нужный фрагмент
        val existing = supportFragmentManager.findFragmentByTag(tag)
        if (existing != null) {
            transaction.show(existing)
        } else {
            val newFragment: Fragment = when (tag) {
                "chat" -> ChatFragment().also { chatFragment = it }
                "documents" -> DocumentsFragment().also { documentsFragment = it }
                "profile" -> ProfileFragment().also { profileFragment = it }
                else -> return
            }
            transaction.add(R.id.fragment_container, newFragment, tag)
        }

        transaction.commit()
    }
}
