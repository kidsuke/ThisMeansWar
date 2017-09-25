package com.datpug

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.RelativeLayout

import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.datpug.ar.ARApplicationControl
import com.datpug.ar.ARApplicationException
import com.datpug.ar.ARApplicationSession
import com.datpug.ar.VuforiaRenderer
import com.vuforia.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers

class AndroidLauncher : AndroidApplication() {
    private var dataSetUserDef: DataSet? = null
    private lateinit var arAppSession: ARApplicationSession
    private lateinit var theGame: ThisMeansWar
    private lateinit var gestureDetector: GestureDetector
    private val disposables: CompositeDisposable = CompositeDisposable()
    var targetBuilderCounter = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arAppSession = ARApplicationSession(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        gestureDetector = GestureDetector(this, GestureListener())

        // Initialize the our grand game \(^_^)/
        val config = AndroidApplicationConfiguration()
        theGame = ThisMeansWar(VuforiaRenderer(arAppSession, Device.MODE.MODE_AR, false))
        initialize(theGame, config)

        // Initialize Vuforia AR
        arAppSession.startVuforia()
            // Need to be done in another thread since it's a blocking call
            .subscribeOn(Schedulers.newThread())
            // Observe the result in main thread
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = {
                    //After starting successfully, activate game AR renderer
                    theGame.arRenderer.setRendererActive(true)
                    // Start user defined targets
                    arAppSession.startUserDefinedTargets()
                    addOverlayView(true)
                },
                onError = {
                    // An error has occurred

                }
            ).addTo(disposables)
    }

    override fun onResume() {
        super.onResume()
        // Resume vuforia
        arAppSession.resumeVuforia()
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
        // Pause vuforia
        arAppSession.pauseVuforia()
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

    fun startBuild() {
        val trackerManager = TrackerManager.getInstance()
        val objectTracker: ObjectTracker? = trackerManager.getTracker(ObjectTracker.getClassType()) as ObjectTracker?

        if (objectTracker != null) {
            val targetBuilder = objectTracker.imageTargetBuilder
            if (targetBuilder != null) {
                var name: String
                do {
                    name = "UserTarget-" + targetBuilderCounter
                    targetBuilderCounter++
                } while (!targetBuilder.build(name, 320.0f))
            }
        }
    }

    // Adds the Overlay view to the GLView
    private fun addOverlayView(initLayout: Boolean) {
        // Inflates the Overlay Layout to be displayed above the Camera View
        val inflater = LayoutInflater.from(this)
        val mUILayout: View = inflater.inflate(R.layout.test_layout, null, false)

        mUILayout.visibility = View.VISIBLE

        // Gets a reference to the Camera button
        val mCameraButton: Button = mUILayout.findViewById(R.id.button)
        mCameraButton.setOnClickListener {
            if (arAppSession.isUserDefinedTargetsRunning())
            arAppSession.buildTrackableSource()
        }
        // Adds the inflated layout to the view
        addContentView(mUILayout, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        mUILayout.bringToFront()
    }
}
