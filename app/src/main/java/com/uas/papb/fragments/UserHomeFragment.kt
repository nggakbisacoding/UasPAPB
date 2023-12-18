package com.uas.papb.fragments

import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.uas.papb.DataListAdapter
import com.uas.papb.R
import com.uas.papb.data.ControllerDB
import com.uas.papb.data.Item
import com.uas.papb.data.ItemDao
import com.uas.papb.databinding.FragmentUserHomeBinding
import com.uas.papb.util.AddOn.isNetworkAvailable
import com.uas.papb.util.NetworkMonitor

class UserHomeFragment : Fragment() {
    private lateinit var binding: FragmentUserHomeBinding
    private lateinit var localdb: ControllerDB
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var manager: RecyclerView.LayoutManager
    private lateinit var myAdapter: RecyclerView.Adapter<*>
    private lateinit var itemDao: ItemDao
    private lateinit var networkMonitor: NetworkMonitor
    private var data: List<Item>? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserHomeBinding.inflate(inflater, container, false)
        val view = inflater.inflate(R.layout.fragment_user_home, container, false)
        localdb = ControllerDB.getDatabase(requireContext())
        firestore = Firebase.firestore
        auth = Firebase.auth
        networkMonitor = NetworkMonitor(requireContext())
        networkMonitor.registerNetworkCallback(networkCallback)

        Thread {
            itemDao = localdb.ItemDao()!!
        }.start()
        recyclerView = view.findViewById(R.id.popular_movies)
        manager = LinearLayoutManager(activity)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        if(auth.currentUser != null) {
            val username = auth.currentUser!!.displayName
            binding.tvUsernameDisplay.text = getString(R.string.hello_username, username)
        }
        getAllData()
    }

    override fun onStop() {
        super.onStop()
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
            val listData = ArrayList<Item>()
            firestore.collection("movie").get().addOnSuccessListener { result ->
                for(document in result) {
                    val obj = document.toObject<Item>()
                    listData.add(obj)
                }
                recyclerView.apply{
                    myAdapter = DataListAdapter(listData)
                    layoutManager = manager
                    adapter = myAdapter
                }
            }
        } else {
            Thread{
                val mdata = itemDao.getAll()
                recyclerView.apply{
                    myAdapter = DataListAdapter(mdata)
                    layoutManager = manager
                    adapter = myAdapter
                }
                data = mdata
            }.start()
        }
    }
}