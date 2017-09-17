package com.datpug.androidcore.app

import android.support.v7.widget.RecyclerView
import com.datpug.androidcore.app.databinding.RecyclerViewBindingViewHolder

/**
 * Created by longvu on 16/09/2017.
 */
abstract class BaseRecyclerViewAdapter: RecyclerView.Adapter<RecyclerViewBindingViewHolder>() {
    override fun getItemViewType(position: Int): Int = getLayoutIdForPosition(position)

    override abstract fun getItemCount(): Int
    abstract protected fun getItemForPosition(position: Int): Any
    abstract protected fun getLayoutIdForPosition(position: Int): Int
}