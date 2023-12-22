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
import com.google.firebase.firestore.FirebaseFirestore
import com.uas.papb.DataListAdapter
import com.uas.papb.data.ControllerDB
import com.uas.papb.data.Item
import com.uas.papb.databinding.FragmentAdminHomeBinding
import com.uas.papb.util.AddOn.isNetworkAvailable
import com.uas.papb.util.NetworkMonitor

class AdminHomeFragment : Fragment() {
    private val firestore = FirebaseFirestore.getInstance()
    private val budgetCollectionRef = firestore.collection("movie")
    private val budgetListLiveData: MutableLiveData<List<Item>> by lazy {
        MutableLiveData<List<Item>>()
    }
    private lateinit var binding: FragmentAdminHomeBinding
    private lateinit var localdb: ControllerDB
    private lateinit var networkMonitor: NetworkMonitor
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAdminHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        localdb = ControllerDB.getDatabase(requireContext())
        networkMonitor = NetworkMonitor(requireContext())
        networkMonitor.registerNetworkCallback(networkCallback)
        with(binding) {
            floatAddBtn.setOnClickListener {
                contentView.visibility = View.GONE
                contentPanel.visibility = View.VISIBLE
                floatAddBtn.visibility = View.GONE
            }
            btnAdd.setOnClickListener {
                val name = edtName.text.toString()
                val desc = edtDesc.text.toString()
                val author = edtAuthor.text.toString()
                val image = edtImage.text.toString()
                val tag = edtTag.text.toString()
                val rating = edtRating.text.toString().toDouble()
                val newItem = Item(name = name, storyline = desc,
                    author = author, image = image, tag = tag, rating = rating, bookmark = false)
                addBudget(newItem)
                contentView.visibility = View.VISIBLE
                contentPanel.visibility = View.GONE
                floatAddBtn.visibility = View.VISIBLE
            }
        }
        observeBudgets()
        getAllBudgets()

    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            Toast.makeText(requireContext(), "Internet on sync local database with firestore", Toast.LENGTH_SHORT).show()
        }

        override fun onLost(network: Network) {
            Toast.makeText(requireContext(), "Internet off use local Room Database", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAllBudgets() {
        if(isNetworkAvailable(requireContext())) {
            observeBudgetChanges()
        } else {
            localdb.ItemDao()!!.allNotes.observe(viewLifecycleOwner) {
                budgetListLiveData.postValue(it)
            }
        }
    }

    private fun observeBudgets() {
        budgetListLiveData.observe(viewLifecycleOwner) { budgets ->
            val adapter = DataListAdapter(budgets)
            binding.listView.adapter = adapter
            binding.listView.layoutManager = LinearLayoutManager(requireContext())
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
                for(i in items) {
                    Thread {
                        if(localdb.ItemDao()!!.selectById(i.id) != null) {
                            localdb.ItemDao()!!.insert(i)
                        }
                    }
                }
            }

        }
    }

    private fun addBudget(item: Item) {
        if(isNetworkAvailable(requireContext())) {
            budgetCollectionRef.add(item)
                .addOnSuccessListener { documentReference ->
                    val createdBudgetId = documentReference.id
                    item.id = createdBudgetId
                    documentReference.set(item)
                        .addOnFailureListener {
                            Log.d("MainActivity", "Error updating budget ID: ", it)
                        }
                }
                .addOnFailureListener {
                    Log.d(TAG, "Error adding budget: ", it)
                }
        } else {
            Thread {
                if(localdb.ItemDao()!!.selectById(item.id) != null) {
                    localdb.ItemDao()!!.update(item)
                } else {
                    localdb.ItemDao()!!.insert(item)
                }
            }
        }
    }

    companion object {
        private const val TAG = "AdminHomeFragment"
    }
}