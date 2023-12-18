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
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.uas.papb.data.ControllerDB
import com.uas.papb.data.User
import com.uas.papb.databinding.ActivityLoginBinding
import com.uas.papb.util.AddOn.isNetworkAvailable
import com.uas.papb.util.NetworkMonitor

class LoginActivity: AppCompatActivity() {
    private lateinit var sharedpref: SharedPreferences
    private lateinit var binding: ActivityLoginBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var etemail: EditText
    private lateinit var etpassword: EditText
    private lateinit var googleLogin: MaterialButton
    private lateinit var db: ControllerDB
    private lateinit var networkMonitor: NetworkMonitor
    companion object {
        const val SHAREDPREF = "shared_keys"
        const val EMAIL = "email"
        const val PASS = "password"
        const val ROLES = "role"
        const val ROLE = "user"
    }
    private var email: String? = null
    private var password: String? = null
    private var role: String? = ROLE
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth
        sharedpref = getSharedPreferences(SHAREDPREF, MODE_PRIVATE)
        db = ControllerDB.getDatabase(applicationContext)
        firestore = Firebase.firestore
        email = sharedpref.getString(EMAIL, null)
        password = sharedpref.getString(PASS, null)
        role = sharedpref.getString(ROLES, ROLE)
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
                checkShared(email!!, password!!)
                checkDataUser()
                updateUI(auth.currentUser)
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

    private fun signin(mail: String, pass: String) {
        if(auth.currentUser != null) {
            val credential = EmailAuthProvider.getCredential(mail, pass)
            auth.currentUser!!.reauthenticate(credential)
        } else {
            auth.signInWithEmailAndPassword(mail, pass).addOnCompleteListener {
                if(it.isSuccessful) {
                    checkShared(mail,pass)
                } else {
                    Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
                    reloadUI()
                }
            }
        }
        checkIfEmailVerified()
    }

    private fun reloadUI() {
        return
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

    private fun updateUI(user: FirebaseUser?) {
        if(user == null) {
            return
        }
        this.startActivity(Intent(baseContext, MainActivity::class.java))
        finish()
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
        if(role != null) {
            firestore.collection("users").document(auth.currentUser?.uid.toString()).get().addOnSuccessListener {
                val dataUser = it.toObject<User>()
                    val roleses = dataUser?.role
                sharedpref.edit().apply {
                    putString(EMAIL, email)
                    putString(PASS, password)
                    putString(ROLE, roleses)
                    apply()
                }
                role = roleses
                }
            }
        else {
            val editor = sharedpref.edit()
            editor.putString(EMAIL, email)
            editor.putString(PASS, password)
            editor.apply()
        }
    }

    private fun checkDataUser() {
        if(role != null) {
            firestore.collection("users").document(auth.currentUser!!.uid).get().addOnSuccessListener { dokumen ->
                role = dokumen.toObject<User>()!!.role
            }
        }
    }
}