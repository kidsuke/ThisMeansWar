package com.datpug.androidcore.app.databinding

import android.databinding.DataBindingUtil
import android.databinding.ViewDataBinding
import android.view.LayoutInflater
import android.view.ViewGroup
import com.datpug.androidcore.app.BaseRecyclerViewAdapter

/**
 * Created by longvu on 16/09/2017.
 */
abstract class RecyclerViewBindingAdapter: BaseRecyclerViewAdapter() {
    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerViewBindingViewHolder {
        val layoutInflater = LayoutInflater.from(parent?.context)
        val binding: ViewDataBinding = DataBindingUtil.inflate(layoutInflater, viewType, parent, false)
        return RecyclerViewBindingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerViewBindingViewHolder, position: Int) {
        holder.bind(getItemForPosition(position))
    }

    override abstract fun getItemCount(): Int
    override abstract fun getItemForPosition(position: Int): Any
    override abstract fun getLayoutIdForPosition(position: Int): Int
}