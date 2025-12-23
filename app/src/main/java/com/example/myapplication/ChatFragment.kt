package com.example.myapplication

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*

class ChatFragment : Fragment() {

    private val messages = mutableListOf<Message>()
    private lateinit var messagesContainer: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: TextView
    private lateinit var newChatButton: TextView
    private lateinit var attachButton: TextView

    // Приветственные элементы
    private lateinit var welcomeContainer: LinearLayout
    private lateinit var chatContainer: FrameLayout
    private var isFirstMessage = true

    data class Message(
        val text: String,
        val isUser: Boolean,
        val time: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        messageInput = view.findViewById(R.id.messageInput)
        sendButton = view.findViewById(R.id.sendButton)
        newChatButton = view.findViewById(R.id.newChatButton)
        attachButton = view.findViewById(R.id.attachButton)

        welcomeContainer = view.findViewById(R.id.welcomeContainer)
        chatContainer = view.findViewById(R.id.messagesContainer)

        initMessagesContainer(view)

        setupClickListeners()

        chatContainer.visibility = View.GONE
        welcomeContainer.visibility = View.VISIBLE
    }

    private fun initMessagesContainer(view: View) {
        chatContainer.removeAllViews()

        scrollView = ScrollView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            isVerticalScrollBarEnabled = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        messagesContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
        }

        scrollView.addView(messagesContainer)
        chatContainer.addView(scrollView)
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun setupClickListeners() {
        sendButton.setOnClickListener {
            sendMessage()
        }

        newChatButton.setOnClickListener {
            clearChat()
        }

        attachButton.setOnClickListener {
            Toast.makeText(requireContext(), "Функция прикрепления файлов", Toast.LENGTH_SHORT).show()
        }

        messageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }

    private fun sendMessage() {
        val text = messageInput.text.toString().trim()
        if (text.isNotEmpty()) {
            if (isFirstMessage) {
                isFirstMessage = false
                welcomeContainer.visibility = View.GONE
                chatContainer.visibility = View.VISIBLE
            }

            addMessage(text, true)
            messageInput.text.clear()
            getAIResponse(text)
        }
    }

    private fun addMessage(text: String, isUser: Boolean) {
        val message = Message(text, isUser)
        messages.add(message)
        addMessageToUI(message)

        scrollView.postDelayed({
            scrollView.fullScroll(View.FOCUS_DOWN)
        }, 100)
    }

    private fun addMessageToUI(message: Message) {
        val messageLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(8)
                bottomMargin = dpToPx(8)
            }
        }

        val messageText = TextView(requireContext()).apply {
            this.text = message.text
            textSize = 16f
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))

            val maxWidth = (resources.displayMetrics.widthPixels * 0.7).toInt()
            this.maxWidth = maxWidth
        }

        val timeText = TextView(requireContext()).apply {
            text = message.time
            textSize = 10f
            setTextColor(0xFF9CA3AF.toInt())
            setPadding(dpToPx(8), dpToPx(2), dpToPx(8), dpToPx(2))
        }

        val textContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        textContainer.addView(messageText)
        textContainer.addView(timeText)

        if (message.isUser) {
            messageLayout.gravity = Gravity.END

            messageLayout.addView(View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, 0).apply {
                    weight = 1f
                }
            })

            messageText.setBackgroundResource(R.drawable.bubble_user)
            messageText.setTextColor(0xFFFFFFFF.toInt())
            messageLayout.addView(textContainer)
        } else {
            messageLayout.gravity = Gravity.START

            val botIcon = TextView(requireContext()).apply {
                text = "🤖"
                textSize = 24f
                setPadding(0, dpToPx(4), dpToPx(8), 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            messageLayout.addView(botIcon)

            messageText.setBackgroundResource(R.drawable.bubble_bot)
            messageText.setTextColor(0xFF000000.toInt())
            messageLayout.addView(textContainer)

            messageLayout.addView(View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, 0).apply {
                    weight = 1f
                }
            })
        }

        messagesContainer.addView(messageLayout)
    }

    private fun clearChat() {
        messages.clear()
        messagesContainer.removeAllViews()
        isFirstMessage = true

        welcomeContainer.visibility = View.VISIBLE
        chatContainer.visibility = View.GONE

        Toast.makeText(requireContext(), "Чат очищен", Toast.LENGTH_SHORT).show()
    }

    private fun getAIResponse(userMessage: String) {
        sendButton.text = "⌛"
        sendButton.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = JSONObject().apply {
                    put("model", "openai/gpt-3.5-turbo")
                    put("max_tokens", 500)

                    val messagesArray = JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", userMessage)
                        })
                    }
                    put("messages", messagesArray)
                }

                // БЕЗОПАСНОЕ ИСПОЛЬЗОВАНИЕ API КЛЮЧА
                val apiKey = BuildConfig.OPENROUTER_API_KEY

                if (apiKey.isEmpty() || apiKey.contains("ваш_ключ_тут")) {
                    withContext(Dispatchers.Main) {
                        addMessage("Ошибка: API ключ не настроен. Настройте файл secrets.properties", false)
                        resetSendButton()
                    }
                    return@launch
                }

                val request = Request.Builder()
                    .url("https://openrouter.ai/api/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val client = OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val aiResponse = parseAIResponse(responseBody)

                    withContext(Dispatchers.Main) {
                        addMessage(aiResponse, false)
                        resetSendButton()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        addMessage("Ошибка API: ${response.code}", false)
                        resetSendButton()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    addMessage("Ошибка соединения", false)
                    resetSendButton()
                }
            }
        }
    }

    private fun resetSendButton() {
        sendButton.text = "↑"
        sendButton.isEnabled = true
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
}
