// клик по аватарке - выбор фото из галереи и подставновка в профиль

package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ProfileFragment : Fragment() {

    private var passportNumber: String? = null
    private var fullName: String = ""
    private var birthDate: String = ""

    private lateinit var currentPhotoPath: String
    private val photoFileName = "profile_photo.jpg"

    private lateinit var settingsButton: ImageButton
    private lateinit var avatar: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var fullNameText: TextView
    private lateinit var birthDateText: TextView
    private lateinit var passportText: TextView
    private lateinit var hintText: TextView
    private lateinit var toolbar: Toolbar

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                loadImageFromUri(uri)
            }
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
                "Разрешение необходимо для выбора фото",
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

        avatar = view.findViewById(R.id.Avatar)
        progressBar = view.findViewById(R.id.progressBar)
        errorText = view.findViewById(R.id.errorText)
        fullNameText = view.findViewById(R.id.fullNameText)
        birthDateText = view.findViewById(R.id.birthDateText)
        passportText = view.findViewById(R.id.passportText)
        hintText = view.findViewById(R.id.hintText)
        toolbar = view.findViewById(R.id.toolbar)
        settingsButton = view.findViewById(R.id.settingsButton)

        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)

        settingsButton.setOnClickListener {
            val intent = Intent(requireContext(), SettingsActivity::class.java)
            startActivity(intent)
        }

        avatar.setOnClickListener {
            checkPermissionAndOpenGallery()
        }

        loadSavedPhoto()

        loadData()
    }
    private fun checkPermissionAndOpenGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }

            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(
                    requireContext(),
                    "Разрешение необходимо для выбора фото из галереи",
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
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
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
                    savePhotoToStorage(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    requireContext(),
                    "Ошибка загрузки изображения",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun savePhotoToStorage(bitmap: Bitmap) {
        viewLifecycleOwner.lifecycleScope.launch(IO) {
            try {
                val file = File(requireContext().filesDir, photoFileName)
                val fos = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                fos.close()
                currentPhotoPath = file.absolutePath
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun loadSavedPhoto() {
        viewLifecycleOwner.lifecycleScope.launch {
            val file = File(requireContext().filesDir, photoFileName)
            if (file.exists()) {
                val bitmap = withContext(IO) {
                    BitmapFactory.decodeFile(file.absolutePath)
                }
                bitmap?.let {
                    avatar.setImageBitmap(it)
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
        avatar.visibility = View.GONE
    }

    private fun showContent() {
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
                fullName = getString(R.string.user_full_name_template)
                birthDate = getString(R.string.user_birthday_template)
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