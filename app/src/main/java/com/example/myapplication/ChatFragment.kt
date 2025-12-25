package com.example.myapplication

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ChatFragment : Fragment() {

    data class Message(
        val text: String,
        val isUser: Boolean,
        val time: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    )

    data class Chat(
        val id: String = UUID.randomUUID().toString(),
        val messages: MutableList<Message> = mutableListOf(),
        val createdAt: String = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date())
    ) {
        fun getTitle(): String {
            val lastUserMessage = messages.lastOrNull { it.isUser }
            return if (lastUserMessage != null) {
                val text = lastUserMessage.text
                if (text.length > 40) text.substring(0, 40) + "..." else text
            } else {
                "Новый чат"
            }
        }

        fun getLastMessageTime(): String {
            return messages.lastOrNull()?.time ?: createdAt
        }

        fun getShortPreview(): String {
            return messages.firstOrNull()?.text?.take(60) ?: "Пустой чат"
        }

        fun isUnread(): Boolean {
            return messages.size == 1 && messages[0].isUser
        }
    }

    private val chatHistory = mutableListOf<Chat>()
    private var currentChatIndex = 0

    private fun getCurrentChat(): Chat {
        if (chatHistory.isEmpty()) {
            chatHistory.add(Chat())
        }
        return chatHistory[currentChatIndex]
    }

    private fun getCurrentMessages(): MutableList<Message> {
        return getCurrentChat().messages
    }

    private lateinit var messagesContainer: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: TextView
    private lateinit var newChatButton: TextView
    private lateinit var attachButton: TextView
    private lateinit var menuButton: TextView

    private lateinit var welcomeContainer: LinearLayout
    private lateinit var chatContainer: FrameLayout

    private lateinit var historyMenu: LinearLayout
    private lateinit var closeMenuButton: TextView
    private lateinit var chatHistoryList: ListView
    private lateinit var historyAdapter: ChatHistoryAdapter

    private var isFirstMessage = true

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
        menuButton = view.findViewById(R.id.menuButton)

        welcomeContainer = view.findViewById(R.id.welcomeContainer)
        chatContainer = view.findViewById(R.id.messagesContainer)

        historyMenu = view.findViewById(R.id.historyMenu)
        closeMenuButton = view.findViewById(R.id.closeMenuButton)
        chatHistoryList = view.findViewById(R.id.chatHistoryList)

        initChatHistory()
        initMessagesContainer(view)

        setupClickListeners()
        setupHistoryMenu()

        chatContainer.visibility = View.GONE
        welcomeContainer.visibility = View.VISIBLE
    }

    private fun initChatHistory() {
        if (chatHistory.isEmpty()) {
            chatHistory.add(Chat())
        }

        historyAdapter = ChatHistoryAdapter(requireContext(), chatHistory)
        chatHistoryList.adapter = historyAdapter
    }

    private inner class ChatHistoryAdapter(
        context: android.content.Context,
        private val chats: List<Chat>
    ) : ArrayAdapter<Chat>(context, 0, chats) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val chat = chats[position]

            val view = convertView ?: LayoutInflater.from(context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)

            val textView = view.findViewById<TextView>(android.R.id.text1)

            // Создаем красивую карточку
            val background = GradientDrawable().apply {
                cornerRadius = dpToPx(12).toFloat()
                if (position == currentChatIndex) {
                    // Текущий чат - желтый
                    setColor(Color.parseColor("#FFF9C4")) // Светло-желтый
                } else if (chat.isUnread()) {
                    // Непрочитанный чат (только вопрос от пользователя)
                    setColor(Color.parseColor("#E8F5E9")) // Светло-зеленый
                } else {
                    // Обычный чат
                    setColor(Color.parseColor("#F3F4F6")) // Серый
                }
                setStroke(dpToPx(1), Color.parseColor("#E5E7EB")) // Серая граница
            }

            textView.apply {
                // Заголовок
                val title = chat.getTitle()
                val time = chat.getLastMessageTime()
                val preview = chat.getShortPreview()

                text = if (chat.messages.isEmpty()) {
                    "Новый чат"
                } else {
                    "$title " +
                            " $time "
                }

                // Внешний вид
                setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
                textSize = 18f
                maxLines = 5
                gravity = Gravity.START
                isSingleLine = false

                // Цвет текста
                if (position == currentChatIndex) {
                    setTextColor(Color.parseColor("#000000")) // Черный для активного
                    setTypeface(null, Typeface.BOLD)
                } else if (chat.isUnread()) {
                    setTextColor(Color.parseColor("#1B5E20")) // Темно-зеленый для непрочитанного
                    setTypeface(null, Typeface.BOLD)
                } else {
                    setTextColor(Color.parseColor("#374151")) // Серый для остальных
                    setTypeface(null, Typeface.NORMAL)
                }


                // Минимальная высота для красоты
                minimumHeight = dpToPx(80)
            }

            return view
        }
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
            createNewChat()
        }

        menuButton.setOnClickListener {
            showHistoryMenu()
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

    private fun setupHistoryMenu() {
        closeMenuButton.setOnClickListener {
            hideHistoryMenu()
        }

        chatHistoryList.setOnItemClickListener { _, _, position, _ ->
            switchToChat(position)
            hideHistoryMenu()
        }
    }

    private fun showHistoryMenu() {
        updateHistoryList()
        historyMenu.visibility = View.VISIBLE
    }

    private fun hideHistoryMenu() {
        historyMenu.visibility = View.GONE
    }

    private fun createNewChat() {
        // Проверка: не создаем новый чат, если текущий пустой
        val currentMessages = getCurrentMessages()
        if (currentMessages.isEmpty()) {
            Toast.makeText(requireContext(), "Текущий чат уже пуст", Toast.LENGTH_SHORT).show()
            return
        }

        // Создаем новый чат только если в текущем есть сообщения
        val newChat = Chat()
        chatHistory.add(newChat)
        currentChatIndex = chatHistory.size - 1

        clearChatUI()

        welcomeContainer.visibility = View.VISIBLE
        chatContainer.visibility = View.GONE
        isFirstMessage = true

        updateHistoryList()
        Toast.makeText(requireContext(), "Новый чат создан", Toast.LENGTH_SHORT).show()
    }

    private fun switchToChat(index: Int) {
        if (index in chatHistory.indices && index != currentChatIndex) {
            currentChatIndex = index
            val chat = getCurrentChat()

            clearChatUI()

            if (chat.messages.isNotEmpty()) {
                welcomeContainer.visibility = View.GONE
                chatContainer.visibility = View.VISIBLE
                isFirstMessage = false

                for (message in chat.messages) {
                    addMessageToUI(message)
                }
            } else {
                welcomeContainer.visibility = View.VISIBLE
                chatContainer.visibility = View.VISIBLE // Изменено: показываем контейнер даже для пустого чата
                isFirstMessage = true
            }

            updateHistoryList()
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
        getCurrentMessages().add(message)
        addMessageToUI(message)

        scrollView.postDelayed({
            scrollView.fullScroll(View.FOCUS_DOWN)
        }, 100)

        updateHistoryList()
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

    private fun clearChatUI() {
        messagesContainer.removeAllViews()
    }

    private fun clearChat() {
        getCurrentMessages().clear()
        clearChatUI()
        isFirstMessage = true

        welcomeContainer.visibility = View.VISIBLE
        chatContainer.visibility = View.GONE

        updateHistoryList()
        Toast.makeText(requireContext(), "Чат очищен", Toast.LENGTH_SHORT).show()
    }

    private fun updateHistoryList() {
        historyAdapter.notifyDataSetChanged()
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
                            put("role", "system")
                            put("content", """
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
                        """.trimIndent())
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", userMessage)
                        })
                    }
                    put("messages", messagesArray)
                }

                // БЕЗОПАСНОЕ ИСПОЛЬЗОВАНИЕ API КЛЮЧА
                val apiKey = BuildConfig.OPENROUTER_API_KEY

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