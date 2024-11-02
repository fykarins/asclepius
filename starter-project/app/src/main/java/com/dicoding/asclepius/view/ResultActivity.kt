package com.dicoding.asclepius.view

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.dicoding.asclepius.R
import com.dicoding.asclepius.databinding.ActivityResultBinding

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar setup
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        val results = intent.getStringExtra("RESULT")
        val imageUri = intent.getStringExtra("IMAGE_URI")

        imageUri?.let {
            binding.resultImage.setImageURI(Uri.parse(it))
        }

        results?.let { displayResults(it) }
    }

    private fun displayResults(results: String) {
        binding.resultText.text = results
    }
}
