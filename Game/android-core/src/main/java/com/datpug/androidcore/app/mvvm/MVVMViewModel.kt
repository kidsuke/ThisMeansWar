package com.datpug.androidcore.app.mvvm

/**
 * @author longv
 * Created on 03-Aug-17.
 */
interface MVVMViewModel {
    fun onCreated() // Called when view model is first created
    fun onResume() // Called when view resumes
    fun onPause() // Called when view pauses
    fun setupView()
    fun onBackPressed() : Boolean = false
    fun onHiddenChanged(hidden : Boolean) {} // Called if view is a fragment
}