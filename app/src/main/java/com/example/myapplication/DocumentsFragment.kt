package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

// унести логику во вьюмодель, выделить state (StateFlow)
class DocumentsFragment : Fragment(R.layout.fragment_documents) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var favoritesButton: ImageView

    private val viewModel: DocumentsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerDocuments)
        favoritesButton = view.findViewById(R.id.btnFavorites)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        favoritesButton.setOnClickListener {
            viewModel.toggleFavoritesMode()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    favoritesButton.setImageResource(
                        if (state.isFavoritesMode) R.drawable.ic_star_filled
                        else R.drawable.ic_star_outline
                    )

                    favoritesButton.setColorFilter(
                        if (state.isFavoritesMode)
                            requireContext().getColor(R.color.yellow_500)
                        else
                            requireContext().getColor(R.color.gray_500)
                    )

                    recyclerView.adapter = DocumentsAdapter(state.visibleDocuments) { doc ->
                        viewModel.toggleFavorite(doc)
                    }
                }
            }
        }
    }

    inner class DocumentsAdapter(
        private val items: List<Document>,
        private val onStarClick: (Document) -> Unit
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
                onStarClick(doc)
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
