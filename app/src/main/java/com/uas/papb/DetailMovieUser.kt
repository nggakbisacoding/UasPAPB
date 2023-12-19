package com.uas.papb

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.uas.papb.databinding.ActivityDetailmovieuserBinding

class DetailMovieUser: AppCompatActivity() {
    private lateinit var binding: ActivityDetailmovieuserBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailmovieuserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val data = intent
        val names = data.getStringExtra("name")
        val authors = data.getStringExtra("author")
        val storylines = data.getStringExtra("storyline")
        val tags = data.getStringExtra("tag")
        val images = data.getStringExtra("image")
        val ratings = data.getDoubleExtra("rating", 4.1)

        with(binding) {
            tvNameDetail.text = names
            tvAuthorDetail.text = authors
            tvTagDetail.text = tags
            tvRatingDetail.text = ratings.toString()
            tvDescDetail.text = storylines
            tvImageDetail.text = images

            addToBookmarks.setOnCheckedChangeListener { _, isChecked ->
                if(isChecked) {
                    Toast.makeText(baseContext, "Added to Bookmark", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(baseContext, "Delete from Bookmark", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object
}