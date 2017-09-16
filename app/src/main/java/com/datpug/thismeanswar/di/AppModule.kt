package com.datpug.thismeanswar.di

import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by longvu on 12/09/2017.
 */
@Module
class AppModule(private val application: Application) {

    @Singleton
    @Provides
    fun providesContext(): Context = application.applicationContext

    @Singleton
    @Provides
    fun providesBluetoothManager(): BluetoothManager = application.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
}