package com.example.myapplication


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DocumentsFragment : Fragment(R.layout.fragment_documents) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var favoritesButton: ImageView

    private var isFavoritesMode = false

    private val documents = mutableListOf(
        Document("Трудовой договор", "20.12.2024"),
        Document("Договор аренды", "21.12.2024"),
        Document("Претензия контрагенту", "22.12.2024"),
        Document("Исковое заявление", "23.12.2024"),
        Document("Соглашение о расторжении", "24.12.2024"),
        Document("Политика конфиденциальности", "25.12.2024"),
        Document("Договор оказания услуг", "26.12.2024"),
        Document("Доверенность", "27.12.2024"),
        Document("Жалоба в госорган", "28.12.2024"),
        Document("Пользовательское соглашение", "29.12.2024")
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerDocuments)
        favoritesButton = view.findViewById(R.id.btnFavorites)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        updateList()

        favoritesButton.setOnClickListener {
            isFavoritesMode = !isFavoritesMode
            favoritesButton.setImageResource(
                if (isFavoritesMode) R.drawable.ic_star_filled
                else R.drawable.ic_star_outline
            )

            favoritesButton.setColorFilter(
                if (isFavoritesMode)
                    requireContext().getColor(R.color.yellow_500)
                else
                    requireContext().getColor(R.color.gray_500)
            )
            updateList()
        }
    }

    private fun updateList() {
        val list = if (isFavoritesMode) {
            documents.filter { it.isFavorite }
        } else {
            documents
        }
        recyclerView.adapter = DocumentsAdapter(list)
    }

    data class Document(
        val title: String,
        val date: String,
        var isFavorite: Boolean = false
    )

    inner class DocumentsAdapter(
        private val items: List<Document>
    ) : RecyclerView.Adapter<DocumentsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.tvTitle)
            val date: TextView = view.findViewById(R.id.tvDate)
            val star: ImageView = view.findViewById(R.id.btnStar)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_document, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val doc = items[position]

            holder.title.text = doc.title
            holder.date.text = doc.date

            holder.star.setImageResource(
                if (doc.isFavorite) R.drawable.ic_star_filled
                else R.drawable.ic_star_outline
            )

            holder.star.setColorFilter(
                if (doc.isFavorite)
                    holder.itemView.context.getColor(R.color.yellow_500)
                else
                    holder.itemView.context.getColor(R.color.gray_400)
            )


            holder.star.setOnClickListener {
                doc.isFavorite = !doc.isFavorite
                notifyItemChanged(position)
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
