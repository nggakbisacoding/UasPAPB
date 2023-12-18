package com.uas.papb

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.uas.papb.data.ControllerDB
import com.uas.papb.data.User
import com.uas.papb.data.UserDao
import com.uas.papb.databinding.ActivitySignupBinding
import com.uas.papb.util.AddOn.isValidString
import com.uas.papb.util.AddOn.notEmpty
import com.uas.papb.util.NetworkMonitor

class SignupActivity: AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private lateinit var sharedpref: SharedPreferences
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var db: ControllerDB
    private lateinit var userDao: UserDao
    private lateinit var etName: TextInputLayout
    private lateinit var etEmail: TextInputLayout
    private lateinit var etPass: TextInputLayout
    private lateinit var etConPass: TextInputLayout
    private lateinit var networkMonitor: NetworkMonitor
    companion object {
        const val SHAREDPREF = "shared_keys"
        const val EMAIL = "email"
        const val PASS = "password"
        const val ROLES = "role"
        const val ROLE = "user"
        const val INTERNET = "internet"
    }
    private var email: String? = null
    private var role: String? = null
    private var password: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedpref = getSharedPreferences(SHAREDPREF, Context.MODE_PRIVATE)
        auth = Firebase.auth
        firestore = Firebase.firestore
        db = ControllerDB.getDatabase(applicationContext)
        email = sharedpref.getString(EMAIL, null)
        password = sharedpref.getString(PASS, null)
        role = sharedpref.getString(ROLES, "user")
        userDao = db.UserDao()!!
        etName = findViewById(R.id.et_name_signup)
        etEmail = findViewById(R.id.et_email_id_signup)
        etPass = findViewById(R.id.et_password_signup)
        etConPass = findViewById(R.id.et_re_password_signup)
        networkMonitor = NetworkMonitor(applicationContext)
        networkMonitor.registerNetworkCallback(networkCallback)

        binding.textviewReferLogin.setOnClickListener {
            startActivity(Intent(applicationContext, LoginActivity::class.java))
            finish()
        }

        binding.btnSignup.setOnClickListener {
            signUp()
        }
    }

    override fun onStart() {
        super.onStart()
        if(email != null) {
            checkUser(email!!, password!!)
        }
    }

    private fun signUp() {
        val usermail = etEmail.editText?.text.toString()
        val userpass = etPass.editText?.text.toString()
        if(!isValidString(usermail)) {
            etEmail.error = "Please enter your Email id"
            etEmail.requestFocus()
            return
        }
        if(userpass != etConPass.editText?.text.toString()) {
            etConPass.error = "Your password doesn't match password confirmation"
            etConPass.requestFocus()
            return
        } else {
            if (userpass.length < 8) {
                etPass.error = "Your password  need 8 character or more"
                etPass.requestFocus()
                return
            }
        }

        if(notEmpty(usermail, userpass)) {
            auth.createUserWithEmailAndPassword(usermail, userpass).addOnCompleteListener {
                if(it.isSuccessful) {
                    Toast.makeText(this, "Signup successful please signin manually", Toast.LENGTH_SHORT).show()
                    sendEmailVerify(auth.currentUser, usermail, userpass)
                } else {
                    Toast.makeText(this, "Signup failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            Toast.makeText(applicationContext, "Internet on sync firestore with local room database", Toast.LENGTH_LONG).show()
            firestore.collection("user").get().addOnSuccessListener { result ->
                for(document in result) {
                    val dataUser = document.toObject<User>()
                    if(userDao.selectById(dataUser.id) == dataUser) {
                        Thread {
                            userDao.update(dataUser)
                        }.start()
                    } else {
                        Thread {
                            userDao.insert(dataUser)
                        }.start()
                    }
                }
            }
        }

        override fun onLost(network: Network) {
            Toast.makeText(applicationContext, "Internet off use local Room Database", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStop() {
        super.onStop()
        networkMonitor.unregisterNetworkCallback(networkCallback)
    }


    private fun sendEmailVerify(user: FirebaseUser?, usermail: String, userpass: String) {
        user?.sendEmailVerification()?.addOnCompleteListener {
            if(it.isSuccessful) {
                Toast.makeText(this, "Signup Successful Please check your email", Toast.LENGTH_LONG).show()
                val collection = firestore.collection("users")
                val name: List<String> = etName.editText?.text.toString().split(" ")
                val editedName = ArrayList<String>()
                for(i in name) {
                    editedName.add(i.replaceFirstChar { firstChar ->
                        firstChar.uppercase()})
                }
                val dataUser = User(
                    id = user.uid,
                    name = editedName.joinToString(separator = " "),
                    email = user.email,
                    password = userpass,
                    profileImage = "https://firebasestorage.googleapis.com/v0/b/eating-go-dabf0.appspot.com/o/file%2FPuraUlunDanuBratan.jpg?alt=media&token=1027db5d-de67-44f7-ad82-38e6921a7d46",
                    role = ROLE
                )
                collection.document(user.uid).set(dataUser)
                Thread {
                    userDao.insert(dataUser)
                }.start()
                checkShared(usermail, userpass)
                updateUI(user)
            }
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if(!user?.isEmailVerified!!) {
            auth.signOut()
            startActivity(Intent(applicationContext, LoginActivity::class.java))
            finish()
        }
    }

    private fun checkUser(email: String, password: String) {
        if(email.isNotBlank() && auth.currentUser != null) {
            if(email == auth.currentUser?.email.toString()) {
                startActivity(Intent(applicationContext, MainActivity::class.java))
                finish()
            } else {
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                    if(it.isSuccessful) {
                        checkShared(email, password)
                        startActivity(Intent(applicationContext, MainActivity::class.java))
                        finish()
                    }
                }
                return
            }
        } else {
            return
        }
    }

    private fun checkShared(mail: String, password: String) {
        if(email == null && auth.currentUser != null) {
            val editor = sharedpref.edit()
            editor.putString(EMAIL, mail)
            editor.putString(PASS, password)
            editor.apply()
        } else {
            val editor = sharedpref.edit()
            editor.clear()
            editor.apply()
        }
    }
}