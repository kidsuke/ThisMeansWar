package com.datpug.androidcore.app.databinding

import android.databinding.ViewDataBinding
import android.support.v7.widget.RecyclerView
import android.util.Log
import com.datpug.androidcore.BR
import com.datpug.androidcore.app.ViewType

/**
 * Created by longvu on 16/09/2017.
 */
class RecyclerViewBindingViewHolder(private val binding: ViewDataBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(data: ViewType) {
        if (!binding.setVariable(BR.data, data)) {
            val layoutName = binding.root.resources.getResourceEntryName(binding.root.id)
            Log.w(BindingAdapters::class.qualifiedName, "There is no variable 'data' in layout " + layoutName)
        }
        binding.executePendingBindings()
    }
}