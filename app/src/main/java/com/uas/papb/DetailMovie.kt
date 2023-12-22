package com.uas.papb

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage
import com.uas.papb.data.ControllerDB
import com.uas.papb.data.Item
import com.uas.papb.databinding.ActivityDetailmovieBinding
import com.uas.papb.util.AddOn.isNetworkAvailable

class DetailMovie: AppCompatActivity() {
    private lateinit var binding: ActivityDetailmovieBinding
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var storageRef: StorageReference
    private val budgetCollectionRef = firestore.collection("movie")
    private lateinit var localdb: ControllerDB
    private var updateId = ""
    private var imageUri = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailmovieBinding.inflate(layoutInflater)
        setContentView(binding.root)
        localdb = ControllerDB.getDatabase(applicationContext)
        storageRef = Firebase.storage.reference

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
            edtImageDetail.setImageURI(Uri.parse(images))

            edtImageDetail.setOnClickListener {
                galleryLauncher.launch("image/*")
            }

            btnUpdate.setOnClickListener {
                val name = edtNameDetail.text.toString()
                val desc = edtDescDetail.text.toString()
                val author = edtAuthorDetail.text.toString()
                val tag = edtTagDetail.text.toString()
                val rating = edtRatingDetail.text.toString().toDouble()
                val itemToUpdate = Item(id=updateId,name = name, storyline = desc,
                    author = author, image = imageUri, tag = tag, rating = rating, bookmark = "false")
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
            edtImageDetail.setImageResource(R.drawable.add_image)
            edtTagDetail.setText("")
            edtAuthorDetail.setText("")
        }
    }

    private fun switch() {
        startActivity(Intent(baseContext, MainActivity::class.java))
        finish()
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        try{
            val sd = getFileName(uri!!)
            storageRef.child("file/$sd.jpg").putFile(uri).addOnSuccessListener { _ ->
                storageRef.child("file/$sd.jpg").downloadUrl.addOnSuccessListener {url ->
                    imageUri = url.toString()
                }
            }
            Glide.with(this).load(imageUri).into(binding.edtImageDetail)
        }catch(e:Exception){
            e.printStackTrace()
        }
    }

    companion object {
        const val TAG = "DetailMovie"
    }

    @SuppressLint("Range")
    private fun getFileName(uri: Uri): String? {
        return uri.path?.lastIndexOf('/')?.let { uri.path?.substring(it) }
    }
}