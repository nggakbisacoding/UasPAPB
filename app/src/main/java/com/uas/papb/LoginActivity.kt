package com.uas.papb

import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.uas.papb.data.ControllerDB
import com.uas.papb.databinding.ActivityLoginBinding
import com.uas.papb.util.AddOn.isNetworkAvailable
import com.uas.papb.util.NetworkMonitor

class LoginActivity: AppCompatActivity() {
    private lateinit var sharedpref: SharedPreferences
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var etemail: EditText
    private lateinit var etpassword: EditText
    private lateinit var googleLogin: MaterialButton
    private lateinit var db: ControllerDB
    private lateinit var networkMonitor: NetworkMonitor
    private var email: String? = null
    private var password: String? = null
    private var role: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth
        sharedpref = getSharedPreferences(SignupActivity.SHAREDPREF, MODE_PRIVATE)
        db = ControllerDB.getDatabase(applicationContext)
        email = sharedpref.getString(SignupActivity.EMAIL, null)
        password = sharedpref.getString(SignupActivity.PASS, null)
        role = sharedpref.getString(SignupActivity.ROLES, null)
        etemail = findViewById(R.id.email)
        etpassword = findViewById(R.id.password)
        googleLogin = findViewById(R.id.google_login)
        networkMonitor = NetworkMonitor(applicationContext)
        networkMonitor.registerNetworkCallback(networkCallback)

        binding.signUp.setOnClickListener {
            startActivity(Intent(applicationContext, SignupActivity::class.java))
            finish()
        }

        binding.forgotPassword.setOnClickListener {
            forgotPassword(email!!)
        }

        googleLogin.setOnClickListener {

        }

        binding.loginBtn.setOnClickListener {
            val email = etemail.text.toString()
            val password = etpassword.text.toString()

            if(email.isBlank() || password.isBlank()) {
                Toast.makeText(applicationContext, "Email or Password cannot empty!", Toast.LENGTH_SHORT).show()
            } else {
                signin(email, password)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if(isNetworkAvailable(applicationContext)) {
            if(email != null) {
                signin(email!!, password!!)
            }
        } else {
            Thread {
                val localuser = db.UserDao()?.findbyEmail(email!!)
                if(!localuser?.email.isNullOrEmpty()) {
                    if(email.equals(localuser?.email) && password?.equals(localuser?.password)!!) {
                        startActivity(Intent(baseContext, MainActivity::class.java))
                        finish()
                    }
                }
            }.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.unregisterNetworkCallback(networkCallback)
    }

    private fun signin(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
            if(it.isSuccessful) {
                checkShared(email,password)
                checkIfEmailVerified()
            } else {
                Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            Toast.makeText(applicationContext, "Internet on use firebase to login", Toast.LENGTH_LONG).show()
            if(auth.currentUser != null) {
                checkIfEmailVerified()
            } else {
                if(email != null || password != null) {
                    signin(email!!, password!!)
                }
            }
        }

        override fun onLost(network: Network) {
            Toast.makeText(applicationContext, "Internet off use local Room Database to login", Toast.LENGTH_SHORT).show()
        }
    }

    private fun forgotPassword(email: String) {
        if(!isNetworkAvailable(applicationContext)) {
            Toast.makeText(applicationContext, "To reset password please make sure internet on", Toast.LENGTH_LONG).show()
            return
        }
        auth.sendPasswordResetEmail(email).addOnCompleteListener {
            if(it.isSuccessful) {
                Toast.makeText(applicationContext, "Check your mail and spam box", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed send reset link", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkIfEmailVerified() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user!!.isEmailVerified) {
            Toast.makeText(this, "Successfully logged in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(baseContext, MainActivity::class.java))
            finish()
        } else {
            FirebaseAuth.getInstance().signOut()
        }
    }

    private fun checkShared(email: String, password: String) {
        if(email.isBlank() && auth.currentUser != null) {
            val editor = sharedpref.edit()
            editor.putString(SignupActivity.EMAIL, email)
            editor.putString(SignupActivity.PASS, password)
            editor.apply()
        } else {
            val editor = sharedpref.edit()
            editor.clear()
            editor.apply()
        }
    }
}