package com.datpug.androidcore.rx

import android.databinding.ObservableField
import android.databinding.Observable.OnPropertyChangedCallback
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable

/**
 * Created by long.vu on 8/3/2017.
 */
class ObservableFieldObservable<T>  (val observableField : ObservableField<T>): Observable<T>() {

    override fun subscribeActual(observer: Observer<in T>?) {
        val callback = object : OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: android.databinding.Observable?, propertyId: Int) {
                observer!!.onNext(observableField.get())
            }
        }
        observer!!.onSubscribe(object : MainThreadDisposable() {
            override fun onDispose() {
                observableField.removeOnPropertyChangedCallback(callback)
            }
        })
        observableField.addOnPropertyChangedCallback(callback)
    }
}