// SettingsActivity.kt
package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.WindowCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // 1. Get the WindowInsetsController
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // 2. Tell it that the appearance is light (so icons should be dark)
        windowInsetsController.isAppearanceLightStatusBars = true

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Настройки"
        toolbar.setNavigationOnClickListener { finish() }

        val recyclerView = findViewById<RecyclerView>(R.id.settingsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val settings = listOf(
            Setting("Язык общения", "Русский"),
            Setting("Тип консультаций", "Гражданское право"),
            Setting("Уведомления", "Включены"),
            Setting("Тема оформления", "Светлая"),
            Setting("Аккаунт", "mikhail@example.com"),
            Setting("О приложении", "Версия 1.2.0")
        )

        recyclerView.adapter = SettingsAdapter(settings)
    }

    data class Setting(val title: String, val subtitle: String)

    class SettingsAdapter(private val items: List<Setting>) :
        RecyclerView.Adapter<SettingsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title = view.findViewById<TextView>(R.id.title)
            val subtitle = view.findViewById<TextView>(R.id.subtitle)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_setting, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.title.text = item.title
            holder.subtitle.text = item.subtitle

            holder.itemView.setOnClickListener { /* TODO */ }
        }

        override fun getItemCount() = items.size
    }
}