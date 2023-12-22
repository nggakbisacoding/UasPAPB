package com.uas.papb

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.uas.papb.data.ControllerDB
import com.uas.papb.databinding.ActivityMainBinding
import com.uas.papb.fragments.AdminHomeFragment
import com.uas.papb.fragments.BookmarkFragment
import com.uas.papb.fragments.ProfileFragment
import com.uas.papb.fragments.UserHomeFragment
import com.uas.papb.util.AddOn.isNetworkAvailable

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    private lateinit var firedb: FirebaseFirestore
    private lateinit var fireauth: FirebaseAuth
    private lateinit var sharedpref: SharedPreferences
    private lateinit var localdb: ControllerDB
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
    private var foundUser: Boolean? = false

    private fun switchFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        firedb = Firebase.firestore
        setContentView(binding.root)
        fireauth = Firebase.auth
        localdb = ControllerDB.getDatabase(applicationContext)
        sharedpref = getSharedPreferences(SHAREDPREF, Context.MODE_PRIVATE)
        email = sharedpref.getString(EMAIL, null)
        password = sharedpref.getString(PASS, null)
        roles = sharedpref.getString(ROLES, null)

        binding.bottomNavigationView.setOnItemSelectedListener{ item ->
            when(item.itemId){
                R.id.home -> if(roles == "user") switchFragment(UserHomeFragment()) else switchFragment(AdminHomeFragment())
                R.id.bookmarks -> if(roles != "user") Toast.makeText(baseContext, "Admin can use this feature", Toast.LENGTH_SHORT).show() else switchFragment(BookmarkFragment())
                R.id.profile -> switchFragment(ProfileFragment())
                else -> {
                    Toast.makeText(baseContext, "Fragment Invalid!", Toast.LENGTH_SHORT).show()
                }
            }
            true
        }
    }

    override fun onStart() {
        super.onStart()
        if(isNetworkAvailable(baseContext)) {
            if(email != fireauth.currentUser?.email.toString()) {
                val editor = sharedpref.edit()
                editor.clear()
                editor.apply()
            }
            val currentUser = fireauth.currentUser
            updateUI(currentUser)
        } else {
            Thread {
                foundUser = localdb.UserDao()?.findbyEmail(email!!) == null
            }
            if(!foundUser!!) {
                Toast.makeText(baseContext, "Cannot find email in localdb please do signup", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if(user == null) {
            startActivity(Intent(this, SplashActivity::class.java))
            finish()
        } else {
            if(roles == "user") switchFragment(UserHomeFragment()) else switchFragment(AdminHomeFragment())
        }
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