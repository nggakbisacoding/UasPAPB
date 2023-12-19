package com.uas.papb

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.uas.papb.data.Item
import com.uas.papb.databinding.ActivityDetailmovieBinding

class DetailMovieUser: AppCompatActivity() {
    private lateinit var binding: ActivityDetailmovieBinding
    private val firestore = FirebaseFirestore.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailmovieBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val data = intent
        val names = data.getStringExtra("name")
        val authors = data.getStringExtra("author")
        val storylines = data.getStringExtra("storyline")
        val tags = data.getStringExtra("tag")
        val images = data.getStringExtra("image")
        val ratings = data.getDoubleExtra("rating", 4.1)

        with(binding) {
            edtNameDetail.setText(names)
            edtAuthorDetail.setText(authors)
            edtTagDetail.setText(tags)
            edtRatingDetail.setText(ratings.toString())
            edtDescDetail.setText(storylines)
            edtImageDetail.setText(images)
        }
    }

    companion object {
        const val TAG = "DetailMovieUser"
    }
}