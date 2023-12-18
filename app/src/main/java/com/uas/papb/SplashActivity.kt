package com.uas.papb

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private lateinit var sharefpref: SharedPreferences
    private lateinit var auth: FirebaseAuth
    companion object {
        const val SHAREDPREF = "shared_keys"
        const val EMAIL = "email"
        const val PASS = "password"
    }
    private var email: String? = null
    private var password: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        auth = Firebase.auth
        sharefpref = getSharedPreferences(SHAREDPREF, Context.MODE_PRIVATE)
        email = sharefpref.getString(EMAIL, null)
        password = sharefpref.getString(PASS, null)

        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if(email != null) {
                startActivity(Intent(baseContext, LoginActivity::class.java))
                finish()
            } else {
                startActivity(Intent(baseContext, SignupActivity::class.java))
                finish()
            }
        }, 2000)
    }

    override fun onStart() {
        super.onStart()
        if(auth.currentUser == null) {
            val editor = sharefpref.edit()
            editor.clear()
            editor.apply()
            return
        }
        if(auth.currentUser != null && email == auth.currentUser?.email.toString()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}