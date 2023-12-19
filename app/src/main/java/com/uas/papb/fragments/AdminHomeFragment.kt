package com.uas.papb.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.FirebaseFirestore
import com.uas.papb.R
import com.uas.papb.data.Item
import com.uas.papb.databinding.FragmentAdminHomeBinding

class AdminHomeFragment : Fragment() {
    private val firestore = FirebaseFirestore.getInstance()
    private val budgetCollectionRef = firestore.collection("movie")
    private var updateId = ""
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
            }
            btnUpdate.setOnClickListener {
                val name = edtName.text.toString()
                val desc = edtDesc.text.toString()
                val author = edtAuthor.text.toString()
                val image = edtImage.text.toString()
                val tag = edtTag.text.toString()
                val rating = edtRating.text.toString().toDouble()
                val itemToUpdate = Item(name = name, storyline = desc,
                    author = author, image = image, tag = tag, rating = rating)
                updateBudget(itemToUpdate)
                updateId = ""
                setEmptyField()
                contentView.visibility = View.VISIBLE
                contentPanel.visibility = View.GONE
            }
            listView.setOnItemClickListener { adapterView, _, i, _ ->
                val item = adapterView.adapter.getItem(i) as Item
                updateId = item.id
                edtName.setText(item.name)
                edtDesc.setText(item.storyline)
                edtAuthor.setText(item.author)
                edtTag.setText(item.tag)
                edtImage.setText(item.image)
                edtRating.setText(item.rating.toString())
            }
            listView.onItemLongClickListener = AdapterView.OnItemLongClickListener {
                    adapterView, _, i, _ ->
                val item = adapterView.adapter.getItem(i) as Item
                deleteBudget(item)
                true
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
            val adapter = ArrayAdapter(requireContext(),
                R.layout.simple_list_data,R.id.tv_name,
                budgets.toMutableList()
            )
            binding.listView.adapter = adapter
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

    private fun updateBudget(item: Item) {
        item.id = updateId
        budgetCollectionRef.document(updateId).set(item)
            .addOnFailureListener {
                Log.d(TAG, "Error updating budget: ", it)
            }
    }

    private fun deleteBudget(item: Item) {
        if (item.id.isEmpty()) {
            Log.d("MainActivity", "Error deleting: budget ID is empty!")
            return
        }
        budgetCollectionRef.document(item.id).delete()
            .addOnFailureListener {
                Log.d("MainActivity", "Error deleting budget: ", it)
            }
    }

    private fun setEmptyField() {
        with(binding) {
            edtName.setText("")
            edtDesc.setText("")
            edtRating.setText("")
            edtImage.setText("")
            edtTag.setText("")
            edtAuthor.setText("")
        }

    }

    companion object {
        private const val TAG = "AdminHomeFragment"
    }
}