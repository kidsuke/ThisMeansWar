package com.datpug.androidcore.app.mvvm

import android.os.Bundle
import android.view.View

import com.datpug.androidcore.app.BaseActivity
import com.datpug.androidcore.app.BaseFragment

import io.reactivex.disposables.CompositeDisposable

/**
 * Created by long.vu on 8/18/2017.
 */
abstract class MVVMFragment<out A : BaseActivity, out VM : MVVMViewModel> : BaseFragment<A>(), MVVMView<VM> {

    protected val disposables: CompositeDisposable = CompositeDisposable()

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.onCreated()
    }

    override fun onResume () {
        super.onResume()
        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (!disposables.isDisposed) disposables.dispose()
        viewModel.onPause()
    }

    override fun onBackPressed () : Boolean = viewModel.onBackPressed()

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        viewModel.onHiddenChanged(hidden)
    }
}