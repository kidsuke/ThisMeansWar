package com.datpug.androidcore.app

import android.databinding.DataBindingUtil
import android.databinding.ViewDataBinding
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup

/**
 * Created by longvu on 16/09/2017.
 */
abstract class BaseRecyclerViewAdapter: RecyclerView.Adapter<BaseRecyclerViewViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): BaseRecyclerViewViewHolder {
        val layoutInflater = LayoutInflater.from(parent?.context)
        val binding: ViewDataBinding = DataBindingUtil.inflate(layoutInflater, viewType, parent, false)
        return BaseRecyclerViewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BaseRecyclerViewViewHolder, position: Int) {
        holder.bind(getItemBindingIdForPosition(position), getItemForPosition(position))
    }

    override fun getItemViewType(position: Int): Int {
        return getLayoutIdForPosition(position)
    }

    override abstract fun getItemCount(): Int
    abstract protected fun getItemBindingIdForPosition(position: Int): Int
    abstract protected fun getItemForPosition(position: Int): Any
    abstract protected fun getLayoutIdForPosition(position: Int): Int
}