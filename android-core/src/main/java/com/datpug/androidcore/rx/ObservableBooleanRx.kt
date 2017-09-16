package com.datpug.androidcore.rx

import android.databinding.ObservableBoolean
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable

/**
 * @author longv
 * Created on 06-Aug-17.
 */
class ObservableBooleanRx(val observableBoolean: ObservableBoolean): Observable<Boolean>() {

    override fun subscribeActual(observer: Observer<in Boolean>?) {
        val callback = object : android.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: android.databinding.Observable?, propertyId: Int) {
                observer!!.onNext(observableBoolean.get())
            }
        }
        observer!!.onSubscribe(object : MainThreadDisposable() {
            override fun onDispose() {
                observableBoolean.removeOnPropertyChangedCallback(callback)
            }
        })
        observableBoolean.addOnPropertyChangedCallback(callback)
    }
}