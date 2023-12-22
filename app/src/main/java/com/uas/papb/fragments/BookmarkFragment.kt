package com.uas.papb.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.uas.papb.DataViewAdapter
import com.uas.papb.data.Item
import com.uas.papb.databinding.FragmentBookmarkBinding
import com.uas.papb.util.AddOn.isNetworkAvailable

class BookmarkFragment : Fragment() {
    private lateinit var binding: FragmentBookmarkBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val itemcollect = firestore.collection("movie")
    private val budgetListLiveData: MutableLiveData<List<Item>> by lazy {
        MutableLiveData<List<Item>>()
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBookmarkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeBudgets()
        getAllBudgets()
    }

    private fun getAllBudgets() {
        if(isNetworkAvailable(requireContext())) {
            observeBudgetChanges()
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
        itemcollect.whereEqualTo("bookmark", "true").addSnapshotListener { snapshots, error ->
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
}