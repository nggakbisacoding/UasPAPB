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
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage
import com.uas.papb.data.ControllerDB
import com.uas.papb.data.User
import com.uas.papb.data.UserDao
import com.uas.papb.databinding.ActivitySignupBinding
import com.uas.papb.util.AddOn.isNetworkAvailable
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
    private lateinit var storageRef: StorageReference
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
        storageRef = Firebase.storage.reference
        db = ControllerDB.getDatabase(applicationContext)
        email = sharedpref.getString(EMAIL, null)
        password = sharedpref.getString(PASS, null)
        role = sharedpref.getString(ROLES, ROLE)
        Thread {
            userDao = db.UserDao()!!
        }.start()
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
        if(email != auth.currentUser?.email) {
            val editor = sharedpref.edit()
            editor.clear()
            editor.apply()
        } else {
            if(email != null) {
                startActivity(Intent(baseContext, LoginActivity::class.java))
                finish()
            }
        }
    }

    private fun signUp() {
        val usermail = etEmail.editText?.text.toString()
        val userpass = etPass.editText?.text.toString()
        if(!isValidString(usermail)) {
            etEmail.error = "Please enter valid email"
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
            val name: List<String> = etName.editText?.text.toString().split(" ")
            val editedName = ArrayList<String>()
            for(i in name) {
                editedName.add(i.replaceFirstChar { firstChar ->
                    firstChar.uppercase()})
            }
            if(isNetworkAvailable(baseContext)) {
                auth.createUserWithEmailAndPassword(usermail, userpass).addOnCompleteListener {
                    if(it.isSuccessful) {
                        val user = auth.currentUser
                        storageRef.child("file/PuraUlunDanuBratan.jpg").downloadUrl.addOnSuccessListener {
                            val profileUpdates = userProfileChangeRequest {
                                displayName = editedName.joinToString(separator = " ")
                                photoUri = it
                            }
                            user?.updateProfile(profileUpdates)
                        }
                        val collection = firestore.collection("users")
                        val dataUser = User(
                            id = user!!.uid,
                            name = editedName.joinToString(separator = " "),
                            email = usermail,
                            password = userpass,
                            profileImage = "https://firebasestorage.googleapis.com/v0/b/eating-go-dabf0.appspot.com/o/file%2FPuraUlunDanuBratan.jpg?alt=media&token=1027db5d-de67-44f7-ad82-38e6921a7d46",
                            role = ROLE
                        )
                        collection.document(user.uid).set(dataUser)
                        Thread {
                            userDao.insert(dataUser)
                        }.start()
                        sendEmailVerify(auth.currentUser, usermail, userpass)
                    } else {
                        Toast.makeText(this, "Signup failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                val dataUser = User(
                    id = "x",
                    name = editedName.joinToString(separator = " "),
                    email = usermail,
                    password = userpass,
                    profileImage = "https://firebasestorage.googleapis.com/v0/b/eating-go-dabf0.appspot.com/o/file%2FPuraUlunDanuBratan.jpg?alt=media&token=1027db5d-de67-44f7-ad82-38e6921a7d46",
                    role = ROLE
                )
                Thread {
                    userDao.insert(dataUser)
                }.start()
                val editor = sharedpref.edit()
                editor.putString(EMAIL, usermail)
                editor.putString(PASS, userpass)
                editor.putString(ROLES, role)
                editor.apply()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            firestore.collection("user").get().addOnSuccessListener { result ->
                for(document in result) {
                    val dataUser = document.toObject<User>()
                    Thread {
                        if(userDao.selectById(dataUser.id) == dataUser) {
                            userDao.update(dataUser)
                        } else {
                            userDao.insert(dataUser)
                        }
                    }.start()
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
                val editor = sharedpref.edit()
                editor.putString(EMAIL, usermail)
                editor.putString(PASS, userpass)
                editor.putString(ROLES, role)
                editor.apply()
                updateUI(user)
            }
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if(!user?.isEmailVerified!!) {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(baseContext, LoginActivity::class.java))
            finish()
        }
        return
    }
}