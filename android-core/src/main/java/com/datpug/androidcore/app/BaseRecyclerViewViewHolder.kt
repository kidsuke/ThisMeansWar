package com.datpug.androidcore.app

import android.databinding.ViewDataBinding
import android.support.v7.widget.RecyclerView

/**
 * Created by longvu on 16/09/2017.
 */
class BaseRecyclerViewViewHolder(private val binding: ViewDataBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(itemId: Int, item: Any) {
        binding.setVariable(itemId, item)
        binding.executePendingBindings()
    }
}