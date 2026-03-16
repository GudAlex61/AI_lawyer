package com.example.myapplication

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class DocumentsUiState(
    val documents: List<Document> = emptyList(),
    val isFavoritesMode: Boolean = false
) {
    val visibleDocuments: List<Document>
        get() = if (isFavoritesMode) documents.filter { it.isFavorite } else documents
}

class DocumentsViewModel : ViewModel() {

    private val initialDocuments = listOf(
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

    private val _state = MutableStateFlow(DocumentsUiState(documents = initialDocuments, isFavoritesMode = false))
    val state: StateFlow<DocumentsUiState> = _state

    fun toggleFavoritesMode() {
        val current = _state.value
        _state.value = current.copy(isFavoritesMode = !current.isFavoritesMode)
    }

    fun toggleFavorite(document: Document) {
        val current = _state.value
        val newDocs = current.documents.map {
            if (it.title == document.title && it.date == document.date) it.copy(isFavorite = !it.isFavorite)
            else it
        }
        _state.value = current.copy(documents = newDocs)
    }
}
