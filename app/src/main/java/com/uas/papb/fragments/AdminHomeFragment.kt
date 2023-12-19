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
import com.uas.papb.databinding.FragmentAdminHomeBinding

class AdminHomeFragment : Fragment() {
    private val firestore = FirebaseFirestore.getInstance()
    private val budgetCollectionRef = firestore.collection("movie")
    private val budgetListLiveData: MutableLiveData<List<Item>> by lazy {
        MutableLiveData<List<Item>>()
    }
    private lateinit var binding: FragmentAdminHomeBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAdminHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
                    author = author, image = image, tag = tag, rating = rating)
                addBudget(newItem)
                contentView.visibility = View.VISIBLE
                contentPanel.visibility = View.GONE
                floatAddBtn.visibility = View.VISIBLE
            }
        }
        observeBudgets()
        getAllBudgets()

    }

    private fun getAllBudgets() {
        observeBudgetChanges()
    }

    private fun observeBudgets() {
        budgetListLiveData.observe(viewLifecycleOwner) { budgets ->
            val adapter = DataViewAdapter(budgets)
            binding.listView.adapter = adapter
            binding.listView.layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeBudgetChanges() {
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

    private fun addBudget(item: Item) {
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
    }

    companion object {
        private const val TAG = "AdminHomeFragment"
    }
}