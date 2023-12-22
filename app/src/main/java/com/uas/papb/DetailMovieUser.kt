package com.uas.papb

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.uas.papb.data.ControllerDB
import com.uas.papb.databinding.ActivityDetailmovieuserBinding
import com.uas.papb.util.NotifReceiver

class DetailMovieUser: AppCompatActivity() {
    private lateinit var binding: ActivityDetailmovieuserBinding
    private lateinit var localdb: ControllerDB
    private val firestore = FirebaseFirestore.getInstance()
    private val collectionMovie = firestore.collection("items")
    private val channelId = "TEST_NOTIF"
    private val notifId = 90
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailmovieuserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val data = intent
        val id = data.getStringExtra("id")
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
            Glide.with(baseContext).load(images).into(tvImageDetail)

            addToBookmarks.setOnCheckedChangeListener { _, isChecked ->
                if(isChecked) {
                    check("Added to Bookmark")
                    collectionMovie.document(id!!).update("bookmark", "true")
                } else {
                    check("Delete from Bookmark")
                    collectionMovie.document(id!!).update("bookmark", "false")
                }
            }
        }
    }

    private fun check(notif: String) {
        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        }
        else {
            0
        }
        val intent = Intent(this,
            NotifReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            flag
        )
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle("Bookmark")
            .setContentText(notif)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(0, "Notif", pendingIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notifChannel = NotificationChannel(
                channelId, // Id channel
                "Bookmark", // Nama channel notifikasi
                NotificationManager.IMPORTANCE_DEFAULT
            )
            with(notifManager) {
                createNotificationChannel(notifChannel)
                notify(notifId, builder.build())
            }
        }
        else {
            notifManager.notify(notifId, builder.build())
        }
    }
}