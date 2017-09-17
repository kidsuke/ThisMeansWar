package com.datpug.androidcore.app.databinding

import android.databinding.BindingAdapter
import android.databinding.ObservableList
import android.databinding.adapters.ListenerUtil
import android.databinding.ViewDataBinding
import android.databinding.DataBindingUtil
import android.view.LayoutInflater

import android.view.ViewGroup
import android.widget.EditText

import com.datpug.androidcore.extension.hideKeyboard
import com.datpug.androidcore.extension.showKeyboard
import com.datpug.androidcore.R

import android.os.Build
import android.transition.TransitionManager
import android.util.Log

/**
 * Created by long.vu on 8/17/2017.
 */
class BindingAdapters {
    companion object {
        @BindingAdapter("showKeyboard")
        @JvmStatic
        fun showKeyboard(editText: EditText, show: Boolean) {
            if (show) editText.showKeyboard()
            else editText.hideKeyboard()
        }

        @BindingAdapter("entries", "layout")
        @JvmStatic
        fun setEntries(viewGroup: ViewGroup,
            oldEntries: ObservableList<Any>?, oldLayoutId: Int,
            newEntries: ObservableList<Any>?, newLayoutId: Int) {
            if (oldEntries == newEntries && oldLayoutId == newLayoutId) {
                //No change occurs
                return
            }

            var listener: EntryChangeListener? = ListenerUtil.getListener(viewGroup, R.id.entryListener)
            if (oldEntries != newEntries && listener != null) {
                oldEntries?.removeOnListChangedCallback(listener)
            }

            if (newEntries == null) {
                viewGroup.removeAllViews()
            } else {
                if (listener == null) {
                    listener = EntryChangeListener(viewGroup, newLayoutId)
                    ListenerUtil.trackListener(viewGroup, listener, R.id.entryListener)
                } else {
                    listener.layoutId = newLayoutId
                }
                if (newEntries !== oldEntries) {
                    newEntries.addOnListChangedCallback(listener)
                }
                resetViews(viewGroup, newLayoutId, newEntries)
            }
        }

        private fun bindLayout(inflater: LayoutInflater, parent: ViewGroup, layoutId: Int, data: Any): ViewDataBinding {
            val binding = DataBindingUtil.inflate<ViewDataBinding>(inflater, layoutId, parent, false)
            if (!binding.setVariable(BR.data, data)) {
                val layoutName = parent.resources.getResourceEntryName(layoutId)
                Log.w(BindingAdapters::class.qualifiedName, "There is no variable 'data' in layout " + layoutName)
            }
            return binding
        }

        private fun resetViews(parent: ViewGroup, layoutId: Int, entries: List<Any>) {
            parent.removeAllViews()
            if (layoutId == 0) {
                return
            }

            val layoutInflater = LayoutInflater.from(parent.context)
            entries
            .map { bindLayout(layoutInflater, parent, layoutId, it) }
            .forEach { parent.addView(it.root) }
        }
    }

    private class EntryChangeListener(val viewGroup: ViewGroup, var layoutId: Int): ObservableList.OnListChangedCallback<ObservableList<Any>>() {

        override fun onChanged(observableList: ObservableList<Any>) {
            resetViews(viewGroup, layoutId, observableList)
        }

        override fun onItemRangeChanged(observableList: ObservableList<Any>, start: Int, count: Int) {
            if (layoutId == 0) {
                return
            }

            startTransition(viewGroup)

            val layoutInflater = LayoutInflater.from(viewGroup.context)
            val end = start + count
            for (i in start until end) {
                val data = observableList[i]
                val binding = bindLayout(layoutInflater, viewGroup, layoutId, data)
                if (!binding.setVariable(BR.data, observableList[i])) {
                    val layoutName = viewGroup.resources.getResourceEntryName(layoutId)
                    Log.w(EntryChangeListener::class.qualifiedName, "There is no variable 'data' in layout " + layoutName)
                }
                viewGroup.removeViewAt(i)
                viewGroup.addView(binding.root, i)
            }
        }

        override fun onItemRangeInserted(observableList: ObservableList<Any>, start: Int, count: Int) {
            if (layoutId == 0) {
                return
            }

            startTransition(viewGroup)

            val layoutInflater = LayoutInflater.from(viewGroup.context)
            observableList.reversed()
            .map { bindLayout(layoutInflater, viewGroup, layoutId, it) }
            .forEach { viewGroup.addView(it.root, start) }
        }

        override fun onItemRangeMoved(observableList: ObservableList<Any>, from: Int, to: Int, count: Int) {
            if (layoutId == 0) {
                return
            }

            startTransition(viewGroup)

            for (i in 0 until count) {
                val view = viewGroup.getChildAt(from)
                viewGroup.removeViewAt(from)
                val destination = if (from > to) to + i else to
                viewGroup.addView(view, destination)
            }
        }

        override fun onItemRangeRemoved(observableList: ObservableList<Any>, start: Int, count: Int) {
            if (layoutId == 0) {
                return
            }

            startTransition(viewGroup)

            for (i in 0 until count) {
                viewGroup.removeViewAt(start)
            }
        }

        private fun startTransition(viewGroup: ViewGroup) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                TransitionManager.beginDelayedTransition(viewGroup)
            }
        }
    }
}