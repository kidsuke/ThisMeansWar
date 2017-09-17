package com.datpug.androidcore.app.databinding

import android.databinding.ViewDataBinding
import android.support.v7.widget.RecyclerView

/**
 * Created by longvu on 16/09/2017.
 */
class RecyclerViewBindingViewHolder(private val binding: ViewDataBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(data: Any) {
        binding.setVariable(BR.data, data)
        binding.executePendingBindings()
    }
}