package com.example.myapplication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ChatViewModel : ViewModel() {

    private val _chatHistory = MutableLiveData<MutableList<Chat>>(mutableListOf(Chat()))
    val chatHistory: LiveData<MutableList<Chat>> = _chatHistory

    private val _currentChatIndex = MutableLiveData(0)
    val currentChatIndex: LiveData<Int> = _currentChatIndex

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun getCurrentChat(): Chat {
        return _chatHistory.value?.getOrElse(_currentChatIndex.value ?: 0) { Chat() } ?: Chat()
    }

    fun sendMessage(text: String, isUser: Boolean) {
        val message = Message(text, isUser)
        getCurrentChat().messages.add(message)
        _chatHistory.value = _chatHistory.value // force update

        if (isUser) {
            getAIResponse(text)
        }
    }

    fun createNewChat() {
        val currentMessages = getCurrentChat().messages
        if (currentMessages.isNotEmpty()) {
            val newChat = Chat()
            _chatHistory.value?.add(newChat)
            _currentChatIndex.value = (_chatHistory.value?.size ?: 1) - 1
            _chatHistory.value = _chatHistory.value
        }
    }

    fun switchToChat(index: Int) {
        if (index in 0 until (_chatHistory.value?.size ?: 0) && index != _currentChatIndex.value) {
            _currentChatIndex.value = index
        }
    }

    private fun getAIResponse(userMessage: String) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("model", "openai/gpt-3.5-turbo")
                    put("max_tokens", 500)

                    val messagesArray = JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", getSystemPrompt())
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", userMessage)
                        })
                    }
                    put("messages", messagesArray)
                }

                val apiKey = BuildConfig.OPENROUTER_API_KEY

                val request = Request.Builder()
                    .url("https://openrouter.ai/api/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val aiResponse = parseAIResponse(responseBody)

                    withContext(Dispatchers.Main) {
                        sendMessage(aiResponse, false)
                        _isLoading.value = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        sendMessage("Ошибка API: ${response.code}", false)
                        _isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    sendMessage("Ошибка соединения", false)
                    _isLoading.value = false
                }
            }
        }
    }

    private fun parseAIResponse(responseBody: String?): String {
        return try {
            val json = JSONObject(responseBody ?: "")
            val choices = json.getJSONArray("choices")
            if (choices.length() > 0) {
                val firstChoice = choices.getJSONObject(0)
                val message = firstChoice.getJSONObject("message")
                message.getString("content").trim()
            } else {
                "Не удалось получить ответ"
            }
        } catch (e: Exception) {
            "Ошибка обработки ответа"
        }
    }

    private fun getSystemPrompt(): String {
        return """
            Ты юридический консультант в чате Android-приложения.
            
            КРИТИЧЕСКИ ВАЖНО: Приложение отображает только ЧИСТЫЙ ТЕКСТ.
            Любые элементы форматирования сломают отображение!
            
            ЖЁСТКИЕ ПРАВИЛА:
            1. НИКАКОЙ РАЗМЕТКИ — ни Markdown, ни HTML
            2. НИКАКИХ СТРОК КОДА, ТАБЛИЦ
            3. Списки делай только через цифры с точкой (1. 2. 3.)
            4. Не используй **жирный**, *курсив*, `код`, ## заголовки
            5. Даже не пытайся "украсить" ответ — это сломает приложение
            
            Примеры ЗАПРЕЩЁННОГО форматирования:
            **Это жирный текст** — ПЛОХО
            *Это курсив* — ПЛОХО
            # Заголовок — ПЛОХО
            
            Примеры РАЗРЕШЁННОГО форматирования:
            Это обычный текст. — ХОРОШО
            1. Первый пункт. — ХОРОШО
            2. Второй пункт. — ХОРОШО
            
            Нарушение этих правил сделает твои ответы бесполезными.
        """.trimIndent()
    }
}