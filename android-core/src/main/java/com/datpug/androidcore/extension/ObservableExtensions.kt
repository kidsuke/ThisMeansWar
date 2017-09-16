package com.datpug.androidcore.extension

import android.databinding.ObservableBoolean
import android.databinding.ObservableField

import com.datpug.androidcore.rx.ObservableBooleanRx
import com.datpug.androidcore.rx.ObservableFieldRx

import io.reactivex.Observable

/**
 * Created by long.vu on 8/3/2017.
 */

fun <T> ObservableField<T>.toRxObservable () : Observable<T> {
    return ObservableFieldRx(this)
}

fun ObservableBoolean.toRxObservable () : Observable<Boolean> {
    return ObservableBooleanRx(this)
}

