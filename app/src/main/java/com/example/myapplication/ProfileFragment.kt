package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {

    private lateinit var avatar: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var fullNameText: TextView
    private lateinit var birthDateText: TextView
    private lateinit var passportText: TextView
    private lateinit var hintText: TextView
    private lateinit var toolbar: Toolbar
    private lateinit var settingsButton: ImageButton

    private lateinit var viewModel: ProfileViewModel

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> loadImageFromUri(uri) }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.request_permission_for_photo),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = FakeProfileRepository(
            defaultFullName = getString(R.string.user_full_name_template),
            defaultBirthDate = getString(R.string.user_birthday_template)
        )

        viewModel = ProfileViewModel(
            repository = repository,
            errorProfileNotFound = getString(R.string.error_load)
        )

        bindViews(view)
        setupToolbar()
        setupClickListeners()
        loadSavedAvatarLocally()
        observeUiState()

        viewModel.loadProfile()
    }

    private fun bindViews(view: View) {
        avatar = view.findViewById(R.id.Avatar)
        progressBar = view.findViewById(R.id.progressBar)
        errorText = view.findViewById(R.id.errorText)
        fullNameText = view.findViewById(R.id.fullNameText)
        birthDateText = view.findViewById(R.id.birthDateText)
        passportText = view.findViewById(R.id.passportText)
        hintText = view.findViewById(R.id.hintText)
        toolbar = view.findViewById(R.id.toolbar)
        settingsButton = view.findViewById(R.id.settingsButton)
    }

    private fun setupToolbar() {
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar
            ?.setDisplayShowTitleEnabled(false)
    }

    private fun setupClickListeners() {
        settingsButton.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
        avatar.setOnClickListener {
            checkPermissionAndOpenGallery()
        }
    }

    private fun loadSavedAvatarLocally() {
        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(IO) {
                viewModel.loadSavedAvatar(requireContext())
            }
            bitmap?.let { avatar.setImageBitmap(it) }
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is ProfileUiState.Loading -> showLoading()
                        is ProfileUiState.Content -> showProfile(state.profile)
                        is ProfileUiState.Error -> showError(state.message)
                    }
                }
            }
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        errorText.visibility = View.GONE
        fullNameText.visibility = View.GONE
        birthDateText.visibility = View.GONE
        passportText.visibility = View.GONE
        hintText.visibility = View.GONE
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        errorText.visibility = View.VISIBLE
        errorText.text = message
        fullNameText.visibility = View.GONE
        birthDateText.visibility = View.GONE
        passportText.visibility = View.GONE
        hintText.visibility = View.GONE
    }

    private fun showProfile(profile: UserProfile) {
        progressBar.visibility = View.GONE
        errorText.visibility = View.GONE
        avatar.visibility = View.VISIBLE
        fullNameText.visibility = View.VISIBLE
        birthDateText.visibility = View.VISIBLE
        passportText.visibility = View.VISIBLE

        fullNameText.text = profile.fullName
        birthDateText.text = getString(R.string.birth_date_label, profile.birthDate)

        if (profile.passportNumber == null) {
            passportText.text = getString(
                R.string.passport_label,
                getString(R.string.not_entered)
            )
            hintText.visibility = View.VISIBLE
            passportText.isEnabled = true
            passportText.setOnClickListener {
                viewModel.generateAndSavePassport()
            }
        } else {
            passportText.text = getString(R.string.passport_label, profile.passportNumber)
            hintText.visibility = View.GONE
            passportText.isEnabled = false
            passportText.setOnClickListener(null)
        }
    }

    private fun checkPermissionAndOpenGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.request_permission_for_photo),
                    Toast.LENGTH_LONG
                ).show()
                requestPermissionLauncher.launch(permission)
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryLauncher.launch(intent)
    }

    private fun loadImageFromUri(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val bitmap = withContext(IO) {
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    BitmapFactory.decodeStream(inputStream)
                }
                bitmap?.let {
                    avatar.setImageBitmap(it)
                    viewModel.saveAvatarFromBitmap(requireContext(), it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_loading_photo),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}