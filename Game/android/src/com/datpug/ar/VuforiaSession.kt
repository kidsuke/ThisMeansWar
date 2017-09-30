package com.datpug.ar

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.Log
import android.view.OrientationEventListener
import android.view.WindowManager
import com.vuforia.*
import com.vuforia.Vuforia
import io.reactivex.Completable


/**
 * Created by longvu on 20/09/2017.
 */
class VuforiaSession(private val activity: Activity, private var screenOrientation: Int):  Vuforia.UpdateCallbackInterface {
    companion object {
        // This key must be provided in order to use Vuforia
        val VUFORIA_KEY = "AQQRe6b/////AAAAGcmQz5avqE61gOA0Z/QZxrZWeh2KZN5w64g7UWeagaUwuYUVH863O90Q4QyYCZWC/OHJgA+aOmwZ6HEMktHer59DuPUUzFhipbLLneJf4kjGVssReqak5oN+muwzWRnq0w+uOXMDFEV+x3H4O84G4h6ptaFZ+l3QXsC1kB6RNdn8/dI+RoWqglK3hQgLfcSkT4lMDSpdIHoMnpED+xmn0uD13Yslt+/tWl4xD0YRVPlYxFMSsdok7ErCYg7/jkqpquxGpbK1jVgnXoBptDKF4zINEZmWzYFnoSh8LNMub0Gig9XobqgKeTtDGPCDQQeygJGMgwQaGiQBGUmP1qY6nk5b1ZLLrrtKoYipHFsAI2SJ"
        // Vuforia initialization flags
        val VUFORIA_FLAG = INIT_FLAGS.GL_20
    }

    /**
     * This class is used for tracking the current state of the Vuforia AR session
     * @property CREATING indicating that a [TrackableSource] is being created
     * @property IDLE indicating that no special events occurs
     */
    enum class SessionState { CREATING, IDLE }

    val LOGTAG: String = ""

    // A flag which determines whether Vuforia has been started
    var isStarted = false
        private set
    // A flag which determines whether the camera is running
    var isCameraRunning = false
        private set
    // A flag which determines whether extending tracking should be used
    var extendTracking: Boolean = true
        private set
    // Holds the current state of the session
    var sessionState: SessionState = SessionState.IDLE
        private set

    // Holds the current user defined targets
    private var dataSetUserDef: DataSet? = null
    // Count the amount of targets that have been created
    private var targetBuilderCounter = 1
    // Holds the camera configuration to use upon resuming
    private var cameraConfig = CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT

    init {
        if (screenOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR && Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
            screenOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        }

        // Use an OrientationChangeListener here to capture all orientation changes.  Android
        // will not send an Activity.onConfigurationChanged() callback on a 180 degree rotation,
        // ie: Left Landscape to Right Landscape.  Vuforia needs to react to this change and the
        // ARApplicationSession needs to update the Projection Matrix.
        val orientationEventListener = object : OrientationEventListener(activity) {
            var lastRotation = -1

            override fun onOrientationChanged(i: Int) {
                val activityRotation = activity.windowManager.defaultDisplay.rotation
                if (lastRotation != activityRotation) {
                    lastRotation = activityRotation
                }
            }
        }

        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable()
        }

        // Apply screen orientation
        activity.requestedOrientation = screenOrientation

        // As long as this window is visible to the user, keep the device's screen turned on and bright
        activity.window.setFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON, WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
    }

    /**
     * Start Vuforia, related to Android life cycle
     */
    fun startVuforia(): Completable {
        return Completable.create { emitter ->
            try {
                // Initialize Vuforia
                initVuforia { !emitter.isDisposed }
                // Initialize trackers. This must be done before starting camera
                initTrackers()
                // Start trackers
                startTrackers()
                // Load trackers data
                loadTrackersData()
                // Register for Vuforia callbacks
                Vuforia.registerCallback(this)
                // Finally, start camera
                startCamera(cameraConfig)

                isStarted = true

                emitter.onComplete()
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }
    }

    /**
     * Resume Vuforia, related to Android life cycle
     */
    fun resumeVuforia(): Completable {
        return Completable.create { emitter ->
            try {
                Vuforia.onResume()
                // We may start the camera only if the Vuforia SDK has already been initialized
                if (isStarted && !isCameraRunning) {
                    startTrackers()
                    startCamera(cameraConfig)
                }

                emitter.onComplete()
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }
    }

    /**
     * Pause Vuforia, related to Android life cycle
     */
    fun pauseVuforia(): Completable {
        return Completable.create { emitter ->
            try {
                if (isStarted) {
                    stopTrackers()
                    stopCamera()
                }
                Vuforia.onPause()

                emitter.onComplete()
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }
    }

    /**
     * Stop Vuforia, related to Android life cycle
     */
    fun stopVuforia(): Completable {
        return Completable.create { emitter ->
            try {
                // Release resources
                unloadTrackersData()
                deinitTrackers()

                emitter.onComplete()
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }
    }

    /**
     * Initialize Vuforia
     * @param onProgress function to keep track of the initializing progress
     */
    private fun initVuforia(onProgress: (Int) -> Boolean) {
        Vuforia.setInitParameters(activity, VUFORIA_FLAG, VUFORIA_KEY)

        var progressValue: Int
        do {
            // Vuforia.init() blocks until an initialization step is
            // complete, then it proceeds to the next step and reports
            // progress in percents (0 ... 100%).
            // If Vuforia.init() returns -1, it indicates an error.
            // Initialization is done when progress has reached 100%.
            progressValue = Vuforia.init()
        } while (!onProgress(progressValue) && progressValue in 0..99)
    }

    /**
     * This function is used to initialize the all type of trackers for AR.
     * It must be called after initialize Vuforia successfully with [initVuforia] and before starting the camera with [startCamera]
     */
    private fun initTrackers() {
        val trackerManager = TrackerManager.getInstance()
        val tracker: Tracker? = trackerManager.initTracker(ObjectTracker.getClassType())
        if (tracker == null) {
            Log.d(LOGTAG, "Failed to initialize ObjectTracker.")
        } else {
            Log.d(LOGTAG, "Successfully initialized ObjectTracker.")
        }
    }

    /**
     * This function is used to start trackers for AR
     */
    fun startTrackers() {
        val objectTracker = TrackerManager.getInstance().getTracker(ObjectTracker.getClassType())
        objectTracker?.start()
    }

    /**
     * This function is used to stop trackers for AR
     */
    private fun stopTrackers() {
        val objectTracker = TrackerManager.getInstance().getTracker(ObjectTracker.getClassType())
        objectTracker?.stop()
    }

    /**
     * This function is used for releasing trackers' resources
     */
    private fun deinitTrackers() {
        TrackerManager.getInstance().deinitTracker(ObjectTracker.getClassType())
    }

    /**
     * This function is used for load trackers' data
     */
    private fun loadTrackersData() {
        val trackerManager = TrackerManager.getInstance()
        val objectTracker: ObjectTracker? = trackerManager.getTracker(ObjectTracker.getClassType()) as ObjectTracker?
        if (objectTracker == null) {
            Log.d(LOGTAG, "Failed to load tracking data set because the ObjectTracker has not been initialized.")
            return
        }

        // Create the data set:
        dataSetUserDef = objectTracker.createDataSet()
        if (dataSetUserDef == null) {
            Log.d(LOGTAG, "Failed to create a new tracking data.")
            return
        }

        if (!dataSetUserDef!!.load("StonesAndChips.xml", STORAGE_TYPE.STORAGE_APPRESOURCE)) {
            Log.d(LOGTAG, "Failed to load StonesAndChips.")
        }

        if (!dataSetUserDef!!.load("Tarmac.xml", STORAGE_TYPE.STORAGE_APPRESOURCE)) {
            Log.d(LOGTAG, "Failed to load Tarmac.")
        }

        if (!objectTracker.activateDataSet(dataSetUserDef)) {
            Log.d(LOGTAG, "Failed to activate data set.")
            return
        }

        Log.d(LOGTAG, "Successfully loaded and activated data set.")
    }

    /**
     * This function is used for unload (or release) trackers' data
     */
    private fun unloadTrackersData() {
        val trackerManager = TrackerManager.getInstance()
        val objectTracker: ObjectTracker? = trackerManager.getTracker(ObjectTracker.getClassType()) as ObjectTracker?
        if (objectTracker == null) {
            Log.d(LOGTAG, "Failed to destroy the tracking data set because the ObjectTracker has not been initialized.")
            return
        }

        if (dataSetUserDef != null) {
            if (objectTracker.getActiveDataSet(0) != null && !objectTracker.deactivateDataSet(dataSetUserDef)) {
                Log.d(LOGTAG, "Failed to destroy the tracking data set because the data set could not be deactivated.")
            }

            if (!objectTracker.destroyDataSet(dataSetUserDef)) {
                Log.d(LOGTAG, "Failed to destroy the tracking data set.")
            }

            Log.d(LOGTAG, "Successfully destroyed the data set.")
            dataSetUserDef = null
        }
    }

    /**
     * Initialize and start the camera
     * @param camera camera config
     */
    private fun startCamera(camera: Int) {
        val error: String

        // Check whether camera has been already running
        if (isCameraRunning) {
            error = "Camera already running, unable to open again"
            Log.e(LOGTAG, error)
            throw ARApplicationException(ARApplicationException.CAMERA_INITIALIZATION_FAILURE, error)
        }

        // Init camera
        if (!CameraDevice.getInstance().init(camera)) {
            error = "Unable to open camera device: " + camera
            Log.e(LOGTAG, error)
            throw ARApplicationException(ARApplicationException.CAMERA_INITIALIZATION_FAILURE, error)
        }

        // Set camera video mode
        if (!CameraDevice.getInstance().selectVideoMode(CameraDevice.MODE.MODE_DEFAULT)) {
            error = "Unable to set video mode"
            Log.e(LOGTAG, error)
            throw ARApplicationException(ARApplicationException.CAMERA_INITIALIZATION_FAILURE, error)
        }

        // Set camera focus mode
        if (!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO)) {
            // If continuous autofocus mode fails, attempt to set to a different mode
            if (!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO)) {
                CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL)
            }
        }

        // Start camera
        if (!CameraDevice.getInstance().start()) {
            error = "Unable to start camera device: " + camera
            Log.e(LOGTAG, error)
            throw ARApplicationException(ARApplicationException.CAMERA_INITIALIZATION_FAILURE, error)
        }

        isCameraRunning = true
    }

    /**
     * This function is used to stop the camera
     */
    private fun stopCamera() {
        if (isCameraRunning) {
            isCameraRunning = false
            CameraDevice.getInstance().stop()
            CameraDevice.getInstance().deinit()
        }
    }

    /**
     * If everything related to UserDefinedTargets feature has been setup, use this function to begin
     * @return a Boolean indicates whether the UserDefinedTargets feature has started successfully
     */
    fun startUserDefinedTargets(): Boolean {
        Log.d(LOGTAG, "startUserDefinedTargets")

        val trackerManager = TrackerManager.getInstance()
        val objectTracker: ObjectTracker? = trackerManager.getTracker(ObjectTracker.getClassType()) as ObjectTracker?
        if (objectTracker != null) {
            val targetBuilder = objectTracker.imageTargetBuilder
            if (targetBuilder != null) {
                // if needed, stop the target builder
                if (targetBuilder.frameQuality != ImageTargetBuilder.FRAME_QUALITY.FRAME_QUALITY_NONE) {
                    targetBuilder.stopScan()
                }
                targetBuilder.startScan()
            }
        } else {
            return false
        }

        return true
    }

    /**
     * Check whether UserDefinedTarget is running
     * @return a Boolean indicates UserDefinedTargets feature is running
     */
    fun isUserDefinedTargetsRunning(): Boolean {
        val trackerManager = TrackerManager.getInstance()
        val objectTracker: ObjectTracker? = trackerManager.getTracker(ObjectTracker.getClassType()) as ObjectTracker?

        if (objectTracker != null) {
            val targetBuilder = objectTracker.imageTargetBuilder
            if (targetBuilder != null) {
                Log.e(LOGTAG, "Quality> " + targetBuilder.frameQuality)
                return targetBuilder.frameQuality != ImageTargetBuilder.FRAME_QUALITY.FRAME_QUALITY_NONE
            }
        }
        return false
    }

    /**
     * A callback listens for Vuforia update event
     * This is where trackables are registered to be tracked
     * @param state Current state of Vuforia
     */
    override fun Vuforia_onUpdate(state: State?) {
        val trackerManager = TrackerManager.getInstance()
        val objectTracker = trackerManager.getTracker(ObjectTracker.getClassType()) as ObjectTracker

        if (dataSetUserDef == null) {
            Log.d(LOGTAG, "Dataset for trackers has not been loaded")
            return
        }

//        if (sessionState == SessionState.CREATING) {
//            val newTrackableSource: TrackableSource? = objectTracker.imageTargetBuilder.trackableSource
//            if (newTrackableSource != null) {
//                Log.d(LOGTAG, "Attempting to transfer the trackable source to the dataset")
//
//                // Deactivate current dataset
//                objectTracker.deactivateDataSet(objectTracker.getActiveDataSet(0))
//
//                // Clear the oldest target if the dataset is full or the dataset
//                // already contains five user-defined targets.
//                if (dataSetUserDef!!.hasReachedTrackableLimit() || dataSetUserDef!!.numTrackables >= 5) {
//                    dataSetUserDef!!.destroy(dataSetUserDef!!.getTrackable(0))
//                }
//
//                if (dataSetUserDef!!.numTrackables > 0) {
//                    // We need to stop the extended tracking for the previous target
//                    // so we can enable it for the new one
//                    val previousCreatedTrackableIndex = dataSetUserDef!!.numTrackables - 1
//
//                    objectTracker.resetExtendedTracking()
//                    dataSetUserDef!!.getTrackable(previousCreatedTrackableIndex).stopExtendedTracking()
//                }
//
//                // Add new trackable source
//                val trackable = dataSetUserDef!!.createTrackable(newTrackableSource)
//                if (extendTracking) {
//                    trackable.startExtendedTracking()
//                }
//
//                // Reactivate current dataset
//                objectTracker.activateDataSet(dataSetUserDef)
//
//                // Update session state
//                sessionState = SessionState.IDLE
//            }
//        }
    }

    fun destroyTrackableSource(id: Int) {
        val trackerManager = TrackerManager.getInstance()
        val objectTracker = trackerManager.getTracker(ObjectTracker.getClassType()) as ObjectTracker

        // Deactivate current dataset
        objectTracker.deactivateDataSet(objectTracker.getActiveDataSet(0))
        // Clear the oldest target if the dataset is full or the dataset
        // already contains five user-defined targets.
        for (index in 0 until dataSetUserDef!!.numTrackables) {
            if (dataSetUserDef!!.getTrackable(index).id == id) {
                dataSetUserDef!!.destroy(dataSetUserDef!!.getTrackable(0))
                break
            }
        }

        // Reactivate current dataset
        objectTracker.activateDataSet(dataSetUserDef)
    }

    /**
     * This function is used to create user defined targets
     */
    fun buildTrackableSource() {
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

                // Update session state
                sessionState = SessionState.CREATING
            }
        }
    }

}