package com.datpug

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log

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

class AndroidLauncher : AndroidApplication(), ARApplicationControl {
    private var dataSetUserDef: DataSet? = null
    private lateinit var arAppSession: ARApplicationSession
    private lateinit var theGame: ThisMeansWar
    private val disposables: CompositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arAppSession = ARApplicationSession(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

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













    override fun doInitTrackers(): Boolean {
        // Initialize the image tracker
        val tracker: Tracker? = TrackerManager.getInstance().initTracker(ObjectTracker.getClassType())
        // Indicate if the trackers were initialized correctly
        var result = tracker != null
        // Log the result
        if (result) Log.d("", "Successfully initialized ObjectTracker")
        else Log.d("", "Failed to initialize ObjectTracker")

        return result
    }

    // Initializes AR application components.
    private fun initApplicationAR() {
        // Do application initialization
//        refFreeFrame = RefFreeFrame(this, vuforiaAppSession)
//        refFreeFrame.init()
//
//        // Create OpenGL ES view:
//        val depthSize = 16
//        val stencilSize = 0
//        val translucent = Vuforia.requiresAlpha()
//
//        mGlView = SampleApplicationGLView(this)
//        mGlView.init(translucent, depthSize, stencilSize)
//
//        mRenderer = UserDefinedTargetRenderer(this, vuforiaAppSession)
//        mRenderer.setTextures(mTextures)
//        mGlView.setRenderer(mRenderer)
    }

    override fun doDeinitTrackers(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun doStartTrackers(): Boolean {
        val objectTracker: ObjectTracker? = TrackerManager.getInstance().getTracker(ObjectTracker.getClassType()) as ObjectTracker
        return objectTracker?.start() ?: false
    }

    override fun doStopTrackers(): Boolean {
        val objectTracker: ObjectTracker? = TrackerManager.getInstance().getTracker(ObjectTracker.getClassType()) as ObjectTracker
        objectTracker?.stop()

        return true
    }

    override fun doLoadTrackersData(): Boolean {
        // Get the image tracker
        val objectTracker: ObjectTracker? = TrackerManager.getInstance().getTracker(ObjectTracker.getClassType()) as ObjectTracker
        if (objectTracker == null) {
            Log.d("", "Failed to load tracking data set because the ObjectTracker has not been initialized.")
            return false
        }

        // Create the data set:
        dataSetUserDef = objectTracker.createDataSet()
        if (dataSetUserDef == null) {
            Log.d("", "Failed to create a new tracking data.")
            return false
        }

        if (!objectTracker.activateDataSet(dataSetUserDef)) {
            Log.d("", "Failed to activate data set.")
            return false
        }

        Log.d("", "Successfully loaded and activated data set.")
        return true
    }

    override fun doUnloadTrackersData(): Boolean {
        // Get the image tracker
        val objectTracker: ObjectTracker? = TrackerManager.getInstance().getTracker(ObjectTracker.getClassType()) as ObjectTracker
        if (objectTracker == null) {
            Log.d("", "Failed to load tracking data set because the ObjectTracker has not been initialized")
            return false
        }

        if (dataSetUserDef != null) {
            if (objectTracker.getActiveDataSet(0) != null && !objectTracker.deactivateDataSet(dataSetUserDef)) {
                Log.d("", "Failed to destroy the tracking data set because the data set could not be deactivated")
                return false
            }

            if (!objectTracker.destroyDataSet(dataSetUserDef)) {
                Log.d("", "Failed to destroy the tracking data set")
                return false
            }

            Log.d("", "Successfully destroyed the data set")
            dataSetUserDef = null
        }

        return true
    }

    override fun onInitARDone(exception: ARApplicationException?) {
        if (exception == null) {
            initApplicationAR()

            //mRenderer.setActive(true)

            // Now add the GL surface view. It is important
            // that the OpenGL ES surface view gets added
            // BEFORE the camera is started and video
            // background is configured.
//            addContentView(mGlView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
//                    ViewGroup.LayoutParams.MATCH_PARENT))
//
//            // Sets the UILayout to be drawn in front of the camera
//            mUILayout.bringToFront()
//
//            // Hides the Loading Dialog
//            loadingDialogHandler
//                    .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG)
//
//            // Sets the layout background to transparent
//            mUILayout.setBackgroundColor(Color.TRANSPARENT)
//
            arAppSession.startAR(CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT)
            val config = AndroidApplicationConfiguration()
            //initialize(ThisMeansWar(VuforiaRenderer(arAppSession, Device.MODE.MODE_AR, false)), config)

//            setSampleAppMenuAdditionalViews()
//            mSampleAppMenu = SampleAppMenu(this, this,
//                    "User Defined Targets", mGlView, mUILayout,
//                    mSettingsAdditionalViews)
//            setSampleAppMenuSettings()

        } else {
//            Log.e(LOGTAG, exception.getString())
//            showInitializationErrorMessage(exception.getString())
        }
    }


    override fun onVuforiaUpdate(state: State) {
    }

    override fun onVuforiaResumed() {
    }

    override fun onVuforiaStarted() {

    }
}
