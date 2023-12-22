package com.uas.papb

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
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
    private lateinit var googleSignInClient: GoogleSignInClient
    companion object {
        const val SHAREDPREF = "shared_keys"
        const val EMAIL = "email"
        const val PASS = "password"
        const val ROLES = "role"
        private const val TAG = "GoogleActivity"

    }
    private var email: String? = null
    private var password: String? = null
    private var role: String? = null
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
        role = sharedpref.getString(ROLES, null)
        etemail = findViewById(R.id.email)
        etpassword = findViewById(R.id.password)
        googleLogin = findViewById(R.id.google_login)
        networkMonitor = NetworkMonitor(applicationContext)
        networkMonitor.registerNetworkCallback(networkCallback)

        binding.signUp.setOnClickListener {
            startActivity(Intent(applicationContext, SignupActivity::class.java))
            finish()
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.forgotPassword.setOnClickListener {
            forgotPassword(email!!)
        }

        googleLogin.setOnClickListener {
            signIn()
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

    override fun onStop() {
        super.onStop()
        networkMonitor.unregisterNetworkCallback(networkCallback)
    }
    // [END onactivityresult]

    // [START auth_with_google]
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    checkDataUser()
                    checkShared(email!!, password!!)
                    checkIfEmailVerified()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    updateUI(null)
                }
            }
    }
    // [END auth_with_google]

    // [START signin]
    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        resultLauncher.launch(signInIntent)
    }
    // [END signin]

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e)
            }
        }
    }


    private fun signin(mail: String, pass: String) {
        if(auth.currentUser != null) {
            val credential = EmailAuthProvider.getCredential(mail, pass)
            auth.currentUser!!.reauthenticate(credential)
            checkDataUser()
        } else {
            auth.signInWithEmailAndPassword(mail, pass).addOnCompleteListener {
                if(it.isSuccessful) {
                    checkDataUser()
                    checkShared(mail,pass)
                    checkIfEmailVerified()
                } else {
                    Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
                }
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
            sendEmailVerify(user)
            FirebaseAuth.getInstance().signOut()
            return
        }
    }

    private fun sendEmailVerify(user: FirebaseUser?) {
        user!!.sendEmailVerification().addOnSuccessListener {
            Toast.makeText(baseContext,"Please verify your email!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkShared(email: String, password: String) {
        if(role == null) {
            firestore.collection("users").document(auth.currentUser?.uid.toString()).get().addOnSuccessListener {
                role = if(it != null) {
                    val dataUser = it.toObject<User>()
                    val roleses = dataUser?.role
                    sharedpref.edit().apply {
                        putString(EMAIL, email)
                        putString(PASS, password)
                        putString(ROLES, roleses)
                        apply()
                    }
                    roleses
                } else {
                    "admin"
                }
            }
        } else {
            val editor = sharedpref.edit()
            editor.putString(EMAIL, email)
            editor.putString(PASS, password)
            editor.apply()
        }
    }

    private fun checkDataUser() {
        val query = firestore.collection("users")
        val uid = auth.currentUser!!.uid
        val dataUser = User(id = auth.currentUser!!.uid,
            name = auth.currentUser!!.displayName,
            email = auth.currentUser!!.email,
            password = password,
            profileImage = "https://firebasestorage.googleapis.com/v0/b/eating-go-dabf0.appspot.com/o/file%2FPuraUlunDanuBratan.jpg?alt=media&token=1027db5d-de67-44f7-ad82-38e6921a7d46",
            role = role)
        if(role == "user") {
            query.document(uid).get().addOnSuccessListener { dokumen ->
                if(dokumen == null) {
                    query.document(uid).set(dataUser)
                    query.document(uid).update("role", "user")
                }
                role = dokumen.toObject<User>()!!.role
            }
        } else {
            query.document(uid).get().addOnSuccessListener { doc ->
                if(doc != null) {
                    val data = doc.toObject<User>()
                    if(data?.role == "user") {
                        role = data.role
                    } else {
                        role = "admin"
                        query.document(uid).update("role", "admin")
                    }
                } else {
                    query.document(uid).set(dataUser)
                    query.document(uid).update("role", "admin")
                    role = "admin"
                }
            }
        }
        val editor = sharedpref.edit()
        editor.putString(ROLES, role)
        editor.apply()
        Thread {
            if(db.UserDao()?.findbyEmail(email!!) == null) {
                db.UserDao()?.insert(dataUser)
            } else {
                db.UserDao()?.update(dataUser)
            }
        }
        println(role)
    }
}