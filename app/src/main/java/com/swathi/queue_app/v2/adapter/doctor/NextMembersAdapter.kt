package com.swathi.queue_app.v2.adapter.doctor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.swathi.queue_app.databinding.ItemUpNextTokenBinding
import com.swathi.queue_app.v2.models.TokenItem

class NextMembersAdapter : ListAdapter<TokenItem,NextMembersAdapter.TokenViewHolder>(TokenDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TokenViewHolder {
        val binding = ItemUpNextTokenBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TokenViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TokenViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TokenViewHolder(private val binding: ItemUpNextTokenBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(token: TokenItem) {
            binding.tvTokenNumber.text = token.tokenNumber
            binding.tvPatientName.text = token.patientName
            binding.tvTokenStatus.text = token.status
        }
    }

    class TokenDiffCallback : DiffUtil.ItemCallback<TokenItem>() {
        override fun areItemsTheSame(oldItem: TokenItem, newItem: TokenItem): Boolean {
            return oldItem.tokenId == newItem.tokenId
        }

        override fun areContentsTheSame(oldItem: TokenItem, newItem: TokenItem): Boolean {
            return oldItem == newItem
        }
    }
}