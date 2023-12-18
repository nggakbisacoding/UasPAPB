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
import com.uas.papb.util.AddOn.isNetworkAvailable

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private var internet: Boolean? = true
    private lateinit var sharefpref: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        sharefpref = getSharedPreferences(SignupActivity.SHAREDPREF, Context.MODE_PRIVATE)

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
            val i = Intent(applicationContext, SignupActivity::class.java)
            startActivity(i)
            finish()
        }, 2000)
    }

    override fun onStart() {
        super.onStart()
        internet = isNetworkAvailable(applicationContext)
        val editor = sharefpref.edit()
        editor.putString(SignupActivity.INTERNET, if(internet as Boolean) "on" else "off")
        editor.apply()
    }
}