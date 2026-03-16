package com.example.myapplication

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer

class ChatFragment : Fragment() {

    private val viewModel: ChatViewModel by viewModels()

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

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { handleSelectedFile(it) }
    }

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

        initMessagesContainer(view)

        setupClickListeners()
        setupHistoryMenu()

        observeViewModel()

        // Initial UI update
        updateUIForCurrentChat()
    }

    private fun observeViewModel() {
        viewModel.chatHistory.observe(viewLifecycleOwner, Observer {
            updateHistoryList()
            updateUIForCurrentChat()
        })

        viewModel.currentChatIndex.observe(viewLifecycleOwner, Observer {
            updateHistoryList()
            updateUIForCurrentChat()
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { loading ->
            sendButton.text = if (loading) "⌛" else "↑"
            sendButton.isEnabled = !loading
        })
    }

    private fun updateUIForCurrentChat() {
        val chat = viewModel.getCurrentChat()
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
            chatContainer.visibility = View.VISIBLE
            isFirstMessage = true
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
        sendButton.setOnClickListener { sendMessage() }

        newChatButton.setOnClickListener {
            viewModel.createNewChat()
        }

        menuButton.setOnClickListener { showHistoryMenu() }

        attachButton.setOnClickListener {
            filePickerLauncher.launch("*/*") // все типы файлов
        }

        messageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else false
        }
    }

    private fun handleSelectedFile(uri: Uri) {
        val fileName = getFileName(uri) ?: "Файл"
        // Отправляем сообщение с именем файла (можно расширить до чтения содержимого)
        viewModel.sendMessage("Прикреплен файл: $fileName", true)
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    private fun setupHistoryMenu() {
        closeMenuButton.setOnClickListener { hideHistoryMenu() }

        chatHistoryList.setOnItemClickListener { _, _, position, _ ->
            viewModel.switchToChat(position)
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

    private fun sendMessage() {
        val text = messageInput.text.toString().trim()
        if (text.isNotEmpty()) {
            viewModel.sendMessage(text, true)
            messageInput.text.clear()
        }
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
            text = message.text
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
                layoutParams = LinearLayout.LayoutParams(0, 0).apply { weight = 1f }
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
                layoutParams = LinearLayout.LayoutParams(0, 0).apply { weight = 1f }
            })
        }

        messagesContainer.addView(messageLayout)

        // Прокрутка вниз
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun clearChatUI() {
        messagesContainer.removeAllViews()
    }

    private fun updateHistoryList() {
        val currentIndex = viewModel.currentChatIndex.value ?: 0
        val chats = viewModel.chatHistory.value ?: listOf()
        historyAdapter = ChatHistoryAdapter(requireContext(), chats, currentIndex)
        chatHistoryList.adapter = historyAdapter
    }
}