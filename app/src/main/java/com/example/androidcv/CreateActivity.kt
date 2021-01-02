package com.example.androidcv

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.activity_create.*
import kotlinx.coroutines.asCoroutineDispatcher
import java.io.File
import java.util.concurrent.Executors

private const val FILE_NAME = "photo.jpg"
private const val PICK_PHOTO_CODE = 42
private const val CAPTURE_PHOTO_CODE = 41

class CreateActivity : AppCompatActivity() {
    private var photoUri: Uri? = null
    private var isRunningModel = false
    private var selectedStyle: String = ""
    private var lastSavedFile = ""
    private lateinit var photoFile: File
    private lateinit var styleImageView: ImageView
    private lateinit var resultImageView: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var viewModel: MLExecutionViewModel
    private lateinit var styleTransferModelExecutor: StyleTransferModelExecutor
    private val inferenceThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        resultImageView = findViewById(R.id.imageViewResult)
        styleImageView = findViewById(R.id.styleView1)
        progressBar = findViewById(R.id.progressCircle)

        progressBar.visibility = View.INVISIBLE

        btnGallery.setOnClickListener {
            Log.i("btnGallery", "Open up image picker on device")
            val imagePickerIntent = Intent(Intent.ACTION_GET_CONTENT)
            imagePickerIntent.type = "image/*"
            if (imagePickerIntent.resolveActivity(this.packageManager) != null) {
                startActivityForResult(imagePickerIntent, PICK_PHOTO_CODE)
            }
        }

        btnCamera.setOnClickListener {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            photoFile = getPhotoFile(FILE_NAME)
            // takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoFile)
            val fileProvider = FileProvider.getUriForFile(this, "com.example.fileprovider", photoFile)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)
            if (takePictureIntent.resolveActivity(this.packageManager) != null) {
                startActivityForResult(takePictureIntent, CAPTURE_PHOTO_CODE)
            } else {
                Toast.makeText(this, "Unable to open camera.", Toast.LENGTH_SHORT).show()
            }
        }

        btnUpload.setOnClickListener {
            handleUploadButtonClick()
        }
    }

    private fun getPhotoFile(fileName: String): File {
        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpg", storageDirectory)
    }

    private fun handleUploadButtonClick() {
        if (imageViewOriginal.drawable == null) {
            Toast.makeText(this, "No photo selected.", Toast.LENGTH_SHORT).show()
            return
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // check the result is requested from the right intent
        if (requestCode == PICK_PHOTO_CODE) {
            // check the user has selected an image
            if (resultCode == Activity.RESULT_OK) {
                // TODO: update Uri to be bitmap
                photoUri = data?.data
                Log.i("btnGallery", "photoUri $photoUri")
                imageViewOriginal.setImageURI(photoUri)
            } else {
                Toast.makeText(this, "Image selection canceled.", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == CAPTURE_PHOTO_CODE && resultCode == Activity.RESULT_OK) {
            val takenImage = BitmapFactory.decodeFile(photoFile.absolutePath)
            imageViewOriginal.setImageBitmap(takenImage)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    // apply the selected style on the content image
    private fun startRunningModel() {
        if (!isRunningModel && lastSavedFile.isNotEmpty() && selectedStyle.isNotEmpty()) {
            enableControls(false)
            setImageView(styleImageView, getUriFromAssetThumb(selectedStyle))
            resultImageView.visibility = View.INVISIBLE
            progressBar.visibility = View.VISIBLE
            viewModel.onApplyStyle( baseContext, lastSavedFile, selectedStyle, styleTransferModelExecutor,
                inferenceThread )
        } else {
            Toast.makeText(this, "Previous Model still running", Toast.LENGTH_SHORT).show()
        }
    }

    // function to control buttons
    private fun enableControls(enable: Boolean) {
        isRunningModel = !enable
        btnCamera.isEnabled = enable
        btnGallery.isEnabled = enable
        btnUpload.isEnabled = enable
    }

    // get style thumbnails from asset
    private fun getUriFromAssetThumb(thumb: String): String {
        return "file:///android_asset/thumbnails/$thumb"
    }

    // set up image to imageView, may need preprocessing
    private fun setImageView(imageView: ImageView, imagePath: String) {

    }

    // fun sendImage(view: View) {
    //     val intent = Intent(this, MainActivity::class.java)
    //     intent.putExtra("resId", R.drawable.image)
    //     startActivity(intent)
    // }
}