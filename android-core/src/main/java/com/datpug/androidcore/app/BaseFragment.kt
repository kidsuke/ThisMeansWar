package com.datpug.androidcore.app

import android.graphics.Typeface
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.View
import android.widget.TextView
import android.view.ViewGroup

import com.datpug.androidcore.extension.flattenChildViews

/**
 * @author longv
 * Created on 05-Aug-17.
 */

abstract class BaseFragment<out A : BaseActivity> : Fragment() {

    @Suppress("UNCHECKED_CAST")
    val hostActivity by lazy {
        activity as A
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    open fun onBackPressed(): Boolean = false

    /**
     * This method is only be called once to initialize view after it is created.
     * Setup UI events, adapter, font, etc. should be placed here.
     */
    abstract fun initView()

    fun setFont(font: String) {
        val rootView = view

        if (rootView is ViewGroup) {
            setFont(font, *rootView.flattenChildViews().toTypedArray())
            return
        }

        if (rootView is View) {
            setFont(font, rootView)
            return
        }
    }

    fun setFont(font: String, vararg views: View) {
        val requestedFont = Typeface.createFromAsset(hostActivity.assets, font)
        views.forEach { if (it is TextView) it.typeface = requestedFont }
    }
}
