package com.uas.papb

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

class MainActivity : AppCompatActivity() {
    private lateinit var firedb: FirebaseFirestore
    private lateinit var fireauth: FirebaseAuth
    private lateinit var sharedpref: SharedPreferences
    private var email: String? = null
    private var roles: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firedb = Firebase.firestore
        setContentView(R.layout.activity_main)
        fireauth = Firebase.auth
        sharedpref = getSharedPreferences(SignupActivity.SHAREDPREF, Context.MODE_PRIVATE)
        email = sharedpref.getString(SignupActivity.EMAIL, null)
        roles = sharedpref.getString(SignupActivity.ROLES, null)
    }

    override fun onStart() {
        super.onStart()
        if(fireauth.currentUser == null) {
            startActivity(Intent(applicationContext, LoginActivity::class.java))
            finish()
        }
    }
}