package com.uas.papb

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.uas.papb.data.User
import com.uas.papb.databinding.ActivityMainBinding
import com.uas.papb.fragments.AdminHomeFragment
import com.uas.papb.fragments.BookmarkFragment
import com.uas.papb.fragments.ProfileFragment
import com.uas.papb.fragments.UserHomeFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    private lateinit var firedb: FirebaseFirestore
    private lateinit var fireauth: FirebaseAuth
    private lateinit var sharedpref: SharedPreferences
    companion object {
        const val SHAREDPREF = "shared_keys"
        const val EMAIL = "email"
        const val PASS = "password"
        const val ROLES = "role"
        const val ROLE = "user"
    }
    private var email: String? = null
    private var password: String? = null
    private var roles: String? = ROLE
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        firedb = Firebase.firestore
        setContentView(binding.root)
        fireauth = Firebase.auth
        sharedpref = getSharedPreferences(SHAREDPREF, Context.MODE_PRIVATE)
        email = sharedpref.getString(EMAIL, null)
        password = sharedpref.getString(PASS, null)
        roles = sharedpref.getString(ROLES, ROLE)
        if(roles != null) {
            firedb.collection("users").document(fireauth.currentUser!!.uid).get().addOnSuccessListener {documentSnapshot ->
                val data = documentSnapshot.toObject<User>()
                roles = data?.role
            }
        }

        binding.bottomNavigationView.setOnItemSelectedListener{ item ->
            when(item.itemId){
                R.id.home -> if(roles == "user") switchFragment(UserHomeFragment()) else switchFragment(AdminHomeFragment())
                R.id.bookmarks -> switchFragment(BookmarkFragment())
                R.id.profile -> switchFragment(ProfileFragment())
                else ->{
                }
            }
            true
        }
    }

    override fun onStart() {
        super.onStart()
        if(email != fireauth.currentUser?.email.toString()) {
            val editor = sharedpref.edit()
            editor.clear()
            editor.apply()
        }
        val currentUser = fireauth.currentUser
        updateUI(currentUser)
    }

    private fun updateUI(user: FirebaseUser?) {
        if(user == null) {
            startActivity(Intent(this, SplashActivity::class.java))
            finish()
        } else {
            if(roles == "user") switchFragment(UserHomeFragment()) else switchFragment(AdminHomeFragment())
        }
    }

    private fun switchFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home -> {
                finish()
                return true
            }
            R.id.bookmarks -> {
                finish()
                return true
            }
            R.id.profile -> {
                finish()
                return true
            }
        }
        return super.onContextItemSelected(item)
    }
}