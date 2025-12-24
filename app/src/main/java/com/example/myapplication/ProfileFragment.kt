package com.example.myapplication

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main

class ProfileFragment : Fragment() {

    private var passportNumber: String? = null
    private var fullName: String = ""
    private var birthDate: String = ""

    private lateinit var avatar: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var fullNameText: TextView
    private lateinit var birthDateText: TextView
    private lateinit var passportText: TextView
    private lateinit var hintText: TextView
    private lateinit var toolbar: Toolbar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        avatar = view.findViewById(R.id.Avatar)
        progressBar = view.findViewById(R.id.progressBar)
        errorText = view.findViewById(R.id.errorText)
        fullNameText = view.findViewById(R.id.fullNameText)
        birthDateText = view.findViewById(R.id.birthDateText)
        passportText = view.findViewById(R.id.passportText)
        hintText = view.findViewById(R.id.hintText)
        toolbar = view.findViewById(R.id.toolbar)

        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        toolbar.title = "Профиль"

        loadData()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.profile_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        errorText.visibility = View.GONE
        fullNameText.visibility = View.GONE
        birthDateText.visibility = View.GONE
        passportText.visibility = View.GONE
        hintText.visibility = View.GONE
        avatar.visibility = View.GONE
    }

    private fun showContent() {
        // 🔒 Защита от обращения к уничтоженному View
        if (isAdded && !isDetached && activity != null) {
            progressBar.visibility = View.GONE
            errorText.visibility = View.GONE
            fullNameText.visibility = View.VISIBLE
            birthDateText.visibility = View.VISIBLE
            passportText.visibility = View.VISIBLE
            avatar.visibility = View.VISIBLE

            fullNameText.text = fullName
            birthDateText.text = getString(R.string.birth_date_label, birthDate)

            if (passportNumber == null) {
                passportText.text = getString(R.string.passport_label, getString(R.string.not_entered))
                hintText.visibility = View.VISIBLE
            } else {
                passportText.text = getString(R.string.passport_label, passportNumber)
                hintText.visibility = View.VISIBLE
            }

            passportText.setOnClickListener {
                if (passportNumber == null) {
                    simulatePassportInput()
                }
            }
        }
    }

    private fun loadData() {
        showLoading()
        viewLifecycleOwner.lifecycleScope.launch {
            delay(2000)
            withContext(IO) {
                fullName = "Короленко Михаил Сергеевич"
                birthDate = "04.08.1998"
                passportNumber = null
            }
            showContent()
        }
    }

    private fun simulatePassportInput() {
        passportText.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            delay(500)
            val newPassport = "8090${(100000..999999).random()}"
            withContext(IO) {
                passportNumber = newPassport
            }
            showContent()
            passportText.isEnabled = true
        }
    }
}