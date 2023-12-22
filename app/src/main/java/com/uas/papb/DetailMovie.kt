package com.uas.papb

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.uas.papb.data.ControllerDB
import com.uas.papb.data.Item
import com.uas.papb.databinding.ActivityDetailmovieBinding
import com.uas.papb.util.AddOn.isNetworkAvailable

class DetailMovie: AppCompatActivity() {
    private lateinit var binding: ActivityDetailmovieBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val budgetCollectionRef = firestore.collection("movie")
    private lateinit var localdb: ControllerDB
    private var updateId = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailmovieBinding.inflate(layoutInflater)
        setContentView(binding.root)
        localdb = ControllerDB.getDatabase(applicationContext)

        val data = intent
        val ids = data.getStringExtra("id")
        val names = data.getStringExtra("name")
        val authors = data.getStringExtra("author")
        val storylines = data.getStringExtra("storyline")
        val tags = data.getStringExtra("tag")
        val images = data.getStringExtra("image")
        val ratings = data.getDoubleExtra("rating", 4.1)

        val dataUser = Item(id= ids!!, name=names, author=authors, storyline=storylines, tag=tags, image=images, rating=ratings)

        with(binding) {

            updateId = ids
            edtNameDetail.setText(names)
            edtAuthorDetail.setText(authors)
            edtTagDetail.setText(tags)
            edtRatingDetail.setText(ratings.toString())
            edtDescDetail.setText(storylines)
            edtImageDetail.setText(images)

            btnUpdate.setOnClickListener {
                val name = edtNameDetail.text.toString()
                val desc = edtDescDetail.text.toString()
                val author = edtAuthorDetail.text.toString()
                val image = edtImageDetail.text.toString()
                val tag = edtTagDetail.text.toString()
                val rating = edtRatingDetail.text.toString().toDouble()
                val itemToUpdate = Item(id=updateId,name = name, storyline = desc,
                    author = author, image = image, tag = tag, rating = rating, bookmark = false)
                updateBudget(itemToUpdate)
                updateId = ""
                setEmptyField()
                Toast.makeText(baseContext, "Update data susscesful", Toast.LENGTH_SHORT).show()
            }
            btnDelete.setOnClickListener {
                deleteBudget(dataUser)
                Toast.makeText(baseContext, "Data delete was susscesful", Toast.LENGTH_SHORT).show()
                switch()
            }
        }
    }

    private fun updateBudget(item: Item) {
        if(isNetworkAvailable(baseContext)) {
            item.id = updateId
            budgetCollectionRef.document(updateId).set(item)
                .addOnFailureListener {
                    Log.d(TAG, "Error updating budget: ", it)
                }
        }
        Thread {
            localdb.ItemDao()?.update(item)
        }.start()
    }

    private fun deleteBudget(item: Item) {
        if(isNetworkAvailable(baseContext)) {
            if (item.id.isEmpty()) {
                Log.d("MainActivity", "Error deleting: budget ID is empty!")
                return
            }
            budgetCollectionRef.document(item.id).delete()
                .addOnFailureListener {
                    Log.d("MainActivity", "Error deleting budget: ", it)
                }
        }
        Thread {
            if(localdb.ItemDao()?.selectById(item.id) == null) {
                return@Thread
            } else {
                localdb.ItemDao()!!.delete(item)
            }
        }.start()
    }

    private fun setEmptyField() {
        with(binding) {
            edtNameDetail.setText("")
            edtDescDetail.setText("")
            edtRatingDetail.setText("")
            edtImageDetail.setText("")
            edtTagDetail.setText("")
            edtAuthorDetail.setText("")
        }
    }

    private fun switch() {
        startActivity(Intent(baseContext, MainActivity::class.java))
        finish()
    }

    companion object {
        const val TAG = "DetailMovie"
    }
}