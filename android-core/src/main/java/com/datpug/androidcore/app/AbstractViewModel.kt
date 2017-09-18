package com.datpug.androidcore.app

import com.datpug.androidcore.app.mvvm.MVVMViewModel
import io.reactivex.disposables.CompositeDisposable

/**
 * @author longv
 * Created on 03-Aug-17.
 */
abstract class AbstractViewModel : MVVMViewModel {

    protected val disposables: CompositeDisposable = CompositeDisposable()

    override fun onCreated() {}

    override fun onResume() {}

    override fun onPause() {
        if (!disposables.isDisposed) disposables.dispose()
    }
}