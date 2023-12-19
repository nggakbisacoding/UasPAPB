package com.uas.papb.fragments

import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.uas.papb.DataListAdapter
import com.uas.papb.DetailMovie
import com.uas.papb.R
import com.uas.papb.data.ControllerDB
import com.uas.papb.data.Item
import com.uas.papb.data.ItemDao
import com.uas.papb.databinding.FragmentUserHomeBinding
import com.uas.papb.util.AddOn.isNetworkAvailable
import com.uas.papb.util.NetworkMonitor

class UserHomeFragment : Fragment(), DataListAdapter.OnRestaurantSelectedListener{
    private lateinit var binding: FragmentUserHomeBinding
    private lateinit var localdb: ControllerDB
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var query: Query? = null
    private var adapter: DataListAdapter? = null
    private lateinit var itemDao: ItemDao
    private lateinit var networkMonitor: NetworkMonitor
    private var data: List<Item>? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserHomeBinding.inflate(inflater, container, false)
        localdb = ControllerDB.getDatabase(requireContext())
        firestore = Firebase.firestore
        auth = Firebase.auth
        networkMonitor = NetworkMonitor(requireContext())
        networkMonitor.registerNetworkCallback(networkCallback)
        Thread {
            itemDao = localdb.ItemDao()!!
        }.start()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        FirebaseFirestore.setLoggingEnabled(true)
    }

    override fun onStart() {
        super.onStart()
        if(auth.currentUser != null) {
            val username = auth.currentUser!!.displayName
            binding.tvUsernameDisplay.text = getString(R.string.hello_username, username)
        }
        adapter?.startListening()
        getAllData()
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
        networkMonitor.unregisterNetworkCallback(networkCallback)
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            firestore.collection("movie").get().addOnSuccessListener { result ->
                for(document in result) {
                    val dataItem = document.toObject<Item>()
                    Thread {
                        if(itemDao.selectById(dataItem.id) == dataItem) {
                            itemDao.update(dataItem)
                        } else {
                            itemDao.insert(dataItem)
                        }
                    }.start()
                }
            }
        }

        override fun onLost(network: Network) {
            Toast.makeText(requireContext(), "Internet off use local Room Database", Toast.LENGTH_SHORT).show()
            Thread {
                data = itemDao.getAll()
            }.start()
        }
    }

    private fun getAllData() {
        if(isNetworkAvailable(requireContext())) {
            query = firestore.collection("movie").orderBy("rating", Query.Direction.DESCENDING)
            query?.let{
                adapter = object: DataListAdapter(it, this@UserHomeFragment) {
                    override fun onDataChanged() {
                        if(itemCount == 0) {
                            binding.popularMovies.visibility = View.GONE
                            binding.viewEmpty.visibility = View.VISIBLE
                        } else {
                            binding.popularMovies.visibility = View.VISIBLE
                            binding.viewEmpty.visibility = View.GONE
                        }
                    }
                }
                binding.popularMovies.adapter= adapter
            }
            binding.popularMovies.layoutManager = LinearLayoutManager(context)
        }
    }

    override fun onRestaurantSelected(restaurant: DocumentSnapshot) {
        // Go to the details page for the selected restaurant
        val data = restaurant.toObject<Item>()
        val intent = Intent(requireContext(), DetailMovie::class.java)
        intent.putExtra("id", data!!.id)
        intent.putExtra("name", data.name)
        intent.putExtra("image", data.image)
        intent.putExtra("author", data.author)
        intent.putExtra("storyline", data.storyline)
        intent.putExtra("tag", data.tag)
        intent.putExtra("rating", data.rating)
        startActivity(intent)
        activity?.finish()
    }
}