package com.datpug.androidcore.app

import android.support.v7.widget.RecyclerView

/**
 * Created by longvu on 16/09/2017.
 */
abstract class BaseRecyclerViewAdapter<VH: RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {

    protected var items: List<ViewType> = listOf()

    override fun getItemViewType(position: Int): Int = getLayoutIdForPosition(position)

    override fun getItemCount(): Int = items.size

    protected fun getItemForPosition(position: Int): ViewType = items[position]

    protected fun getLayoutIdForPosition(position: Int): Int = getItemForPosition(position).getLayoutId()
}