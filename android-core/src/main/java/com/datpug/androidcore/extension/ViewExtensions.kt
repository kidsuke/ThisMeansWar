package com.datpug.androidcore.extension

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

/**
 * Created by long.vu on 8/17/2017.
 */

fun EditText.showKeyboard() {
    if (this.hasFocus()) {
        val imm = this.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(this.windowToken, 0)
    }
}

fun EditText.hideKeyboard() {
    if (this.requestFocus()) {
        val imm = this.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }
}

fun ViewGroup.flattenChildViews(): List<View> {
    val views = mutableListOf<View>()
    for (i in 0..(this.childCount - 1)) {
        val child = this.getChildAt(i)
        views.add(child)
        if (child is ViewGroup) views.addAll(child.flattenChildViews())
    }
    return views
}
