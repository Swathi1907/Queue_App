package com.swathi.queue_app.adapter.admin

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.swathi.queue_app.R
import com.swathi.queue_app.databinding.ItemCancelledBinding
import com.swathi.queue_app.databinding.ItemMembersBinding
import com.swathi.queue_app.model.MemberModel
import com.swathi.queue_app.model.QueueItem
import com.swathi.queue_app.model.UserQueueMember


class MemberAdapter(
    private val items: List<QueueItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    inner class MemberViewHolder(val binding: ItemMembersBinding)
        : RecyclerView.ViewHolder(binding.root)

    inner class CancelledViewHolder(val binding: ItemCancelledBinding)
        : RecyclerView.ViewHolder(binding.root)
    companion object {
        private const val TYPE_MEMBER = 0
        private const val TYPE_CANCELLED = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is QueueItem.MemberItem -> TYPE_MEMBER
            is QueueItem.CancelledItem -> TYPE_CANCELLED
        }
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        return if (viewType == TYPE_MEMBER) {

            val binding = ItemMembersBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

            MemberViewHolder(binding)

        } else {

            val binding = ItemCancelledBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

            CancelledViewHolder(binding)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {

        when (val item = items[position]) {

            is QueueItem.MemberItem -> {

                val member = item.member
                val binding = (holder as MemberViewHolder).binding
binding.tvMemberName.text=member.name
                binding.tvTokenNumber.text = member.tokenNumber.toString()
                Log.d(
                    "MEMBER",
                    "name=${member.name}, isMe=${member.isMe}, status=${member.status}"
                )
                if (member.isMe) {

                    binding.cardQueueMember.setCardBackgroundColor(
                        ContextCompat.getColor(holder.itemView.context, R.color.light_blue)
                    )

                    binding.tvTokenNumber.setBackgroundResource(R.drawable.token_circle_me)
                    binding.tvTokenNumber.setTextColor(Color.BLACK)

                    binding.tvBadge.text =
                        if (member.status == "serving") {
                            binding.tvBadge.setTextColor(
                                ContextCompat.getColor(holder.itemView.context, R.color.white)
                            )
                            "YOU • SERVING"


                        }
                        else {
                            binding.tvBadge.setTextColor(Color.WHITE)

                            "YOU • WAITING"
                        }
                    binding.tvBadge.setBackgroundResource(R.drawable.badge_blue)


                } else {

                    binding.cardQueueMember.setCardBackgroundColor(Color.WHITE)

                    binding.tvTokenNumber.setBackgroundResource(R.drawable.token_circle_others)
                    binding.tvTokenNumber.setTextColor(Color.BLACK)

                    if (member.status == "serving") {

                        binding.tvBadge.text = "Serving"
                        binding.tvBadge.setBackgroundResource(R.drawable.bg_status_serving)
                        binding.tvBadge.setTextColor(
                            ContextCompat.getColor(holder.itemView.context, R.color.green_dark)
                        )

                    } else {

                        binding.tvBadge.text = member.status
                        binding.tvBadge.setBackgroundResource(R.drawable.bg_status_waiting)
                        binding.tvBadge.setTextColor(
                            ContextCompat.getColor(holder.itemView.context, R.color.blue_dark)
                        )
                    }
                }
                // Move your existing YOU / SERVING code here
            }

            is QueueItem.CancelledItem -> {

                val binding = (holder as CancelledViewHolder).binding

                binding.tvCancelled.text = "Cancelled Tokens"
                binding.tvTokens.text = item.tokens
            }
        }
    }


    override fun getItemCount() = items.size
}