package com.example.androidcv

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fabCreate.setOnClickListener {
            val intent = Intent(this, CreateActivity::class.java)
            startActivity(intent)
        }
    }

    // private fun startRunningModel() {
    //     if (!isRunningModel && lastSavedFile.isNotEmpty() && selectedStyle.isNotEmpty()) {
    //         val chooseStyleLabel: TextView = findViewById(R.id.choose_style_text_view)
    //         chooseStyleLabel.visibility = View.GONE
    //         enableControls(false)
    //         setImageView(styleImageView, getUriFromAssetThumb(selectedStyle))
    //         resultImageView.visibility = View.INVISIBLE
    //         progressBar.visibility = View.VISIBLE
    //         viewModel.onApplyStyle(
    //             baseContext, lastSavedFile, selectedStyle, styleTransferModelExecutor,
    //             inferenceThread
    //         )
    //     } else {
    //         Toast.makeText(this, "Previous Model still running", Toast.LENGTH_SHORT).show()
    //     }
    // }
}