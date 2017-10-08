package com.datpug.presentation

import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.*
import bolts.Continuation
import bolts.Task

import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.datpug.RemoteController
import com.datpug.ThisMeansWar
import com.datpug.ar.VuforiaSession
import com.datpug.ar.VuforiaRenderer
import com.datpug.entity.Direction
import com.mbientlab.metawear.Data
import com.mbientlab.metawear.MetaWearBoard
import com.mbientlab.metawear.Route
import com.mbientlab.metawear.Subscriber
import com.mbientlab.metawear.android.BtleService
import com.mbientlab.metawear.builder.RouteBuilder
import com.mbientlab.metawear.builder.RouteComponent
import com.mbientlab.metawear.data.Acceleration
import com.mbientlab.metawear.module.Accelerometer
import com.mbientlab.metawear.module.Debug
import com.mbientlab.metawear.module.Led
import com.mbientlab.metawear.module.Logging
import com.vuforia.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.annotations.NonNull
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.functions.Predicate
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.ArrayList
import java.util.concurrent.TimeUnit

class GameLauncher : AndroidApplication(), ServiceConnection, RemoteController {
    private lateinit var vuforiaSession: VuforiaSession
    private lateinit var theGame: ThisMeansWar
    private lateinit var gestureDetector: GestureDetector
    private var mwBoard: MetaWearBoard? = null
    private var accelerometer: Accelerometer? = null
    private val accelerationSubject = PublishSubject.create<Acceleration>()
    private val disposables: CompositeDisposable = CompositeDisposable()
    private var macAddress = ""
    private val operations: MutableList<Pair<String, () -> Unit>> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharePref = getSharedPreferences(applicationContext.packageName, Context.MODE_PRIVATE)
        macAddress = sharePref.getString("macAddress", "")

        vuforiaSession = VuforiaSession(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        gestureDetector = GestureDetector(this, GestureListener())

        // Initialize the our grand game \(^_^)/
        val config = AndroidApplicationConfiguration()
        theGame = ThisMeansWar(VuforiaRenderer(vuforiaSession, Device.MODE.MODE_AR, false))
        initialize(theGame, config)

        applicationContext.bindService(Intent(this, BtleService::class.java), this, Context.BIND_AUTO_CREATE)
        // Initialize Vuforia AR
        vuforiaSession.startVuforia()
            // Need to be done in another thread since it's a blocking call
            .subscribeOn(Schedulers.newThread())
            // Observe the result in main thread
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = {
                    //After starting successfully, activate game AR renderer
                    theGame.arRenderer.setRendererActive(true)
                },
                onError = {
                    // An error has occurred

                }
            ).addTo(disposables)
    }

    override fun onResume() {
        super.onResume()
        // Resume Vuforia
        vuforiaSession.resumeVuforia()
            // Need to be done in another thread since it's a blocking call
            .subscribeOn(Schedulers.newThread())
            // Observe the result in main thread
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = {
                    // Resume completed
                },
                onError = {
                    // An error has occurred
                }
            ).addTo(disposables)
    }

    override fun onPause() {
        super.onPause()
        // Pause Vuforia
        vuforiaSession.pauseVuforia()
            // Need to be done in another thread since it's a blocking call
            .subscribeOn(Schedulers.newThread())
            // Observe the result in main thread
            .observeOn(AndroidSchedulers.mainThread())
            // After pausing the vuforia completes, clear all the disposables
            .doAfterTerminate { disposables.clear() }
            .subscribeBy(
                onComplete = {
                    // Pause completed
                },
                onError = {
                    // An error has occurred
                }
            ).addTo(disposables)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop bluetooth service
        applicationContext.unbindService(this)
        // Stop Vuforia
        vuforiaSession.stopVuforia()
            // Need to be done in another thread since it's a blocking call
            .subscribeOn(Schedulers.newThread())
            // Observe the result in main thread
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = {
                    // Stop completed
                },
                onError = {
                    // An error has occurred
                }
            ).addTo(disposables)
    }

    override fun onServiceConnected(p0: ComponentName?, binder: IBinder?) {
        val serviceBinder = binder as BtleService.LocalBinder

        val mwMacAddress = "C5:71:71:99:C4:D4"   ///< Put your board's MAC address here
        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val btDevice = btManager.adapter.getRemoteDevice(mwMacAddress)

        mwBoard = serviceBinder.getMetaWearBoard(btDevice)
        mwBoard?.connectAsync()
        ?.onSuccessTask {
            accelerometer = mwBoard!!.getModule(Accelerometer::class.java)
            accelerometer!!.configure().odr(25f).range(4f).commit()

            accelerometer!!.acceleration().addRouteAsync({ source ->
                source.stream { data, env ->
                    val acceleration = data.value(Acceleration::class.java)
                    accelerationSubject.onNext(acceleration)
                }
            })
        }
        ?.continueWith({ task ->
            if (task.isFaulted) {
                Log.e(GameLauncher::class.java.canonicalName, if (mwBoard!!.isConnected()) "Error setting up route" else "Error connecting", task.error)
            } else {
                Log.i(GameLauncher::class.java.canonicalName, "Connected")
                theGame.setRemoteController(this)
            }
        })
    }

    override fun startRemoteControl() {
        accelerometer?.start()
        accelerometer?.acceleration()?.start()
    }

    override fun stopRemoteControl() {
        accelerometer?.stop()
        accelerometer?.acceleration()?.stop()
    }

    override fun getRemoteDirection(): Observable<Direction> {
        return accelerationSubject
        .filter({ acceleration -> Math.abs(acceleration.z()) >= 1f || acceleration.y() <= -2f || acceleration.y() >= 0f })
        .throttleFirst(460, TimeUnit.MILLISECONDS)
        .map {
            if (Math.abs(it.z()) > Math.abs(it.y() + 1)) {
                if (it.z() >= 1f) {
                    println("LEFT")
                    Direction.LEFT
                } else {
                    println("RIGHT")
                    Direction.RIGHT
                }
            } else {
                if (it.y() <= -2f) {
                    println("UP")
                    Direction.UP
                } else {
                    println("DOWN")
                    Direction.DOWN
                }
            }
        }
    }

    override fun onServiceDisconnected(p0: ComponentName?) {}

    override fun onTouchEvent(event: MotionEvent?): Boolean = gestureDetector.onTouchEvent(event)

    private inner class GestureListener: GestureDetector.SimpleOnGestureListener() {
        // Used to set autofocus one second after a manual focus is triggered
        private val autofocusHandler = Handler()

        override fun onDown(e: MotionEvent?): Boolean = true

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            val result = CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO)
            if (!result) Log.e("SingleTapUp", "Unable to trigger focus")

            // Generates a Handler to trigger continuous auto-focus after 1 sec
            autofocusHandler.postDelayed({
                val autofocusResult = CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO)
                if (!autofocusResult) Log.e("SingleTapUp", "Unable to re-enable continuous auto-focus")
            }, 1000L)

            return true
        }
    }
}
