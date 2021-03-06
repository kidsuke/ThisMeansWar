package com.datpug.presentation

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.*
import android.widget.TextView

import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.datpug.R
import com.datpug.controller.RemoteController
import com.datpug.ThisMeansWar
import com.datpug.ar.VuforiaSession
import com.datpug.ar.VuforiaRenderer
import com.datpug.entity.Direction
import com.mbientlab.metawear.MetaWearBoard
import com.mbientlab.metawear.android.BtleService
import com.mbientlab.metawear.data.Acceleration
import com.mbientlab.metawear.module.Accelerometer
import com.vuforia.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class GameLauncher : AndroidApplication(), ServiceConnection, RemoteController, SensorEventListener {

    private lateinit var loadingView: View
    private lateinit var progressText: TextView

    private lateinit var vuforiaSession: VuforiaSession
    private lateinit var theGame: ThisMeansWar
    private lateinit var gestureDetector: GestureDetector
    private var sensorManager: SensorManager? = null
    private var environmentSensor: Sensor? = null
    private var mwBoard: MetaWearBoard? = null
    private var accelerometer: Accelerometer? = null
    private val accelerationSubject = PublishSubject.create<Acceleration>()
    private val disposables: CompositeDisposable = CompositeDisposable()
    private var macAddress = ""
    private var enableRemoteControl = false
    private val operations: MutableList<Pair<String, () -> Unit>> = mutableListOf()
    private var operationAt = 0
    private var btServiceStarted = false;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Init needed views
        loadingView = LayoutInflater.from(context).inflate(R.layout.loading_layout, null, false)
        progressText = loadingView.findViewById(R.id.progressText)

        // Get settings info from pref
        val sharePref = getSharedPreferences(applicationContext.packageName, Context.MODE_PRIVATE)
        macAddress = sharePref.getString("macAddress", "C5:71:71:99:C4:D4")
        enableRemoteControl = sharePref.getBoolean("remoteControl", false)

        vuforiaSession = VuforiaSession(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        gestureDetector = GestureDetector(this, GestureListener())
        createOperations()

        // Initialize the our grand game \(^_^)/
        val config = AndroidApplicationConfiguration()
        theGame = ThisMeansWar(VuforiaRenderer(vuforiaSession, Device.MODE.MODE_AR, false))
        initialize(theGame, config)

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
                    // Start operations if has any
                    showLoadingView()
                    publishOperation(operationAt)
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
        if (btServiceStarted) applicationContext.unbindService(this)
        // Stop internal sensor
        sensorManager?.unregisterListener(this, environmentSensor)
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

    override fun exit() {
        Handler().post { theGame.dispose() }
        super.exit()
    }

    private fun showLoadingView() {
        addContentView(loadingView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        loadingView.bringToFront()
    }

    private fun hideLoadingView() {
        loadingView.visibility = View.GONE
    }

    private fun createOperations() {
        // Initialize operation for remote control
        if (enableRemoteControl) {
            operations.add(Pair(getString(R.string.initialize_remote_control)) {
                applicationContext.bindService(Intent(this, BtleService::class.java), this, Context.BIND_AUTO_CREATE)
                btServiceStarted = true
                return@Pair
            })
        }

        // Initialize operation for light sensor
        val permission: Boolean;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permission = Settings.System.canWrite(context);
        } else {
            permission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
        }
        if (permission) {
            operations.add(Pair(getString(R.string.initialize_light_sensor)) {
                sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
                environmentSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_LIGHT)
                sensorManager!!.registerListener(this, environmentSensor, SensorManager.SENSOR_DELAY_NORMAL)
                publishOperation(++operationAt)
                return@Pair
            })
        }
    }

    private fun publishOperation(operationAt: Int) {
        if (operationAt < operations.size) {
            progressText.text = operations[operationAt].first
            operations[operationAt].second()
        } else {
            hideLoadingView()
            theGame.isGameReady = true
        }
    }

    override fun onServiceConnected(p0: ComponentName?, binder: IBinder?) {
        val serviceBinder = binder as BtleService.LocalBinder

        val mwMacAddress = macAddress   ///< Put your board's MAC address here
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
            Handler(Looper.getMainLooper()).post { publishOperation(++operationAt) }
        })
    }

    override fun onServiceDisconnected(p0: ComponentName?) {}

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
        .throttleFirst(480, TimeUnit.MILLISECONDS)
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

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        val sensorValue = sensorEvent.values[0]
        try {
            if (sensorValue <= 50) {
                Settings.System.putInt(this.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 255)
            } else if (sensorValue <= 200) {
                Settings.System.putInt(this.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 150)
            } else if (sensorValue <= 500) {
                Settings.System.putInt(this.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 100)
            } else {
                Settings.System.putInt(this.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 50)
            }

            val brightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            val layoutParams = window.attributes
            layoutParams.screenBrightness = brightness.toFloat() / 255
            window.attributes = layoutParams
        } catch (e: Exception) { }
    }

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
