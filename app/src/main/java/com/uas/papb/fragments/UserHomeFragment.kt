package com.uas.papb.fragments

import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.uas.papb.DataViewAdapter
import com.uas.papb.R
import com.uas.papb.data.ControllerDB
import com.uas.papb.data.Item
import com.uas.papb.data.ItemDao
import com.uas.papb.databinding.FragmentUserHomeBinding
import com.uas.papb.util.AddOn.isNetworkAvailable
import com.uas.papb.util.NetworkMonitor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class UserHomeFragment : Fragment(){
    private lateinit var binding: FragmentUserHomeBinding
    private lateinit var localdb: ControllerDB
    private lateinit var auth: FirebaseAuth
    private lateinit var executorService: ExecutorService
    private val firestore = FirebaseFirestore.getInstance()
    private val budgetCollectionRef = firestore.collection("movie")
    private val budgetListLiveData: MutableLiveData<List<Item>> by lazy {
        MutableLiveData<List<Item>>()
    }
    private lateinit var itemDao: ItemDao
    private lateinit var networkMonitor: NetworkMonitor
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserHomeBinding.inflate(inflater, container, false)
        localdb = ControllerDB.getDatabase(requireContext())
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
        executorService = Executors.newSingleThreadExecutor()

        observeBudgets()
        getAllBudgets()
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

    private fun getAllBudgets() {
        if(isNetworkAvailable(requireContext())) {
            observeBudgetChanges()
        } else {
            itemDao.allNotes.observe(viewLifecycleOwner) {
                println(it)
                budgetListLiveData.postValue(it)
            }
        }
    }

    private fun observeBudgets() {
        budgetListLiveData.observe(viewLifecycleOwner) { budgets ->
            val adapter = DataViewAdapter(budgets)
            binding.popularMovies.adapter = adapter
            binding.popularMovies.layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeBudgetChanges() {
        if(!isNetworkAvailable(requireContext())) {
            return
        }
        budgetCollectionRef.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.d("MainActivity", "Error listening for budget changes: ", error)
                return@addSnapshotListener
            }
            val items = snapshots?.toObjects(Item::class.java)
            if (items != null) {
                budgetListLiveData.postValue(items)
            }
        }
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
                val data = localdb.ItemDao()!!.allNotes.value
                println(data)
                budgetListLiveData.postValue(data)
            }
        }
    }

    private fun getAllData() {
        if(!isNetworkAvailable(requireContext())) {
            return
        } else {
            budgetListLiveData.observe(viewLifecycleOwner) {
                for(i in it) {
                    executorService.execute{localdb.ItemDao()?.insert(i)}
                }
            }
        }
    }
}