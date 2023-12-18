package com.uas.papb

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.uas.papb.data.User
import com.uas.papb.databinding.AccountDetailBinding
import java.util.concurrent.Executors
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage

class AccountDetailActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: AccountDetailBinding
    private lateinit var fname: EditText
    private lateinit var lname: EditText
    private lateinit var phones: EditText
    private var imageUris: Uri? = null
    private lateinit var pass: EditText
    private lateinit var confPass: EditText
    private lateinit var currentpass: EditText
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storageRef: StorageReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AccountDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth
        firestore = Firebase.firestore
        storageRef = Firebase.storage.reference

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        fname = findViewById(R.id.first_name)
        lname = findViewById(R.id.last_name)
        phones = findViewById(R.id.phone_number)
        pass = findViewById(R.id.password)
        confPass = findViewById(R.id.conf_password)
        currentpass = findViewById(R.id.current_password)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStart() {
        super.onStart()
        val name: List<String> = auth.currentUser?.displayName.toString().split(" ")
        fname.text = name[0].toEditable()
        lname.text = name[1].toEditable()

        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        var image: Bitmap?
        var url: String
        firestore.collection("users").document(auth.currentUser!!.uid).get().addOnSuccessListener {
            url = it.toObject<User>()?.profileImage!!
            executor.execute {
                try {
                    val `in` = java.net.URL(url).openStream()
                    image = BitmapFactory.decodeStream(`in`)
                    handler.post {
                        binding.profileImage.setImageBitmap(image)
                    }
                }
                catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        binding.profileImage.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.updateBtn.setOnClickListener {
            if(pass.text.toString().isNotBlank()) {
                checkValue()
            }
            val profiles =  userProfileChangeRequest {
                displayName = fname.text.toString()+" "+lname.text.toString()
                photoUri = Uri.parse(imageUris.toString())
            }
            auth.currentUser?.updateProfile(profiles)?.addOnCompleteListener {
                Toast.makeText(baseContext, "Profile Updates Successfully", Toast.LENGTH_SHORT).show()
            }
            if(currentpass.text.toString().isNotBlank()) {
                updatePrivateData()
            }
        }

        binding.deleteAccountBtn.setOnClickListener {
            binding.confirmDelete.visibility = View.VISIBLE
            binding.confirmDelete.focusable = View.FOCUSABLE
            binding.contentLayout.visibility = View.GONE
            binding.sure.setOnClickListener {
                firestore.collection("users").document(auth.currentUser!!.uid).delete()
                auth.currentUser!!.delete().addOnCompleteListener {
                    if(it.isSuccessful) {
                        Toast.makeText(baseContext, "Account Delete Successfully", Toast.LENGTH_LONG).show()
                        startActivity(Intent(baseContext, SplashActivity::class.java))
                        finish()
                    }
                }
            }
            binding.cancel.setOnClickListener {
                binding.confirmDelete.visibility = View.GONE
                binding.confirmDelete.focusable = View.NOT_FOCUSABLE
                binding.contentLayout.visibility = View.VISIBLE
            }
        }
    }

    private fun String.toEditable(): Editable =  Editable.Factory.getInstance().newEditable(this)

    private fun checkValue() {
        if(pass.text.toString() != confPass.text.toString()) {
            Toast.makeText(baseContext, "Password Confirm Doesn't Match", Toast.LENGTH_LONG).show()
            return
        } else if(pass.text.toString().length < 8) {
            Toast.makeText(baseContext, "Password Need 8 Char or More!", Toast.LENGTH_LONG).show()
            return
        }
    }

    private fun updatePrivateData() {
        val user = auth.currentUser
        val credential = EmailAuthProvider.getCredential(user?.email.toString(), currentpass.text.toString())
        user?.reauthenticate(credential)?.addOnCompleteListener {
            if(it.isSuccessful) {
                Log.d("value", "user re-authenticated")
                user.updatePassword(pass.text.toString())
            }
        }
            ?.addOnFailureListener {
                Toast.makeText(baseContext, it.message, Toast.LENGTH_LONG).show()
            }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        try{
            val sd = getFileName(uri!!)
            storageRef.child("file/$sd.jpg").putFile(uri).addOnSuccessListener { _ ->
                storageRef.child("file/$sd.jpg").downloadUrl.addOnSuccessListener {url ->
                    val photoUri: String?
                    photoUri = url.toString()
                    imageUris = Uri.parse(photoUri)
                    firestore.collection("users").document(auth.currentUser!!.uid).set("profileImage" to imageUris)
                }
            }
            binding.profileImage.setImageURI(uri)
        }catch(e:Exception){
            e.printStackTrace()
        }
    }
    @SuppressLint("Range")
    private fun getFileName(uri: Uri): String? {
        return uri.path?.lastIndexOf('/')?.let { uri.path?.substring(it) }
    }
}