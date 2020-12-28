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
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.activity_create.*
import java.io.File

private const val FILE_NAME = "photo.jpg"
private const val PICK_PHOTO_CODE = 42
private const val CAPTURE_PHOTO_CODE = 41
private lateinit var photoFile: File

class CreateActivity : AppCompatActivity() {
    private var photoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

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
        if (imageView.drawable == null) {
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
                photoUri = data?.data
                Log.i("btnGallery", "photoUri $photoUri")
                imageView.setImageURI(photoUri)
            } else {
                Toast.makeText(this, "Image selection canceled.", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == CAPTURE_PHOTO_CODE && resultCode == Activity.RESULT_OK) {
            val takenImage = BitmapFactory.decodeFile(photoFile.absolutePath)
            imageView.setImageBitmap(takenImage)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

}