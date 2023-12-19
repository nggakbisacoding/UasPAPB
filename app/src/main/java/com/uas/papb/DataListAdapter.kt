package com.uas.papb

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import com.uas.papb.data.Item
import com.uas.papb.databinding.UserItemHolderBinding
import com.uas.papb.util.FirestoreAdapter

open class DataListAdapter(query: Query, private val listener: OnRestaurantSelectedListener) : FirestoreAdapter<DataListAdapter.ViewHolder>(query) {
    interface OnRestaurantSelectedListener {

        fun onRestaurantSelected(data: DocumentSnapshot)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        return ViewHolder(UserItemHolderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false))
        }


    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getSnapshot(position), listener)
    }

    // Holds the views for adding it to image and text
    class ViewHolder(private val binding: UserItemHolderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(snapshot: DocumentSnapshot,
                 listener: OnRestaurantSelectedListener?
        ) {
            val movie = snapshot.toObject<Item>() ?: return

            binding.tvMovNameUser.text = movie.name
            binding.tvMovRatUser.text = movie.rating.toString()
            Glide.with(binding.ivMovImageUser.context).load(Uri.parse(movie.image)).override(350,120).into(binding.ivMovImageUser)

            binding.root.setOnClickListener {
                listener?.onRestaurantSelected(snapshot)
            }
        }
    }
}