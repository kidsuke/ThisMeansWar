package com.datpug.ar

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.AsyncTask
import android.os.Build
import android.util.Log
import android.view.OrientationEventListener
import android.view.WindowManager
import com.datpug.R
import com.vuforia.*
import com.vuforia.Vuforia
import io.reactivex.Completable


/**
 * Created by longvu on 20/09/2017.
 */
class ARApplicationSession(val arAppControl: ARApplicationControl, var screenOrientation: Int):  Vuforia.UpdateCallbackInterface {
    private val VUFORIA_KEY = "AQQRe6b/////AAAAGcmQz5avqE61gOA0Z/QZxrZWeh2KZN5w64g7UWeagaUwuYUVH863O90Q4QyYCZWC/OHJgA+aOmwZ6HEMktHer59DuPUUzFhipbLLneJf4kjGVssReqak5oN+muwzWRnq0w+uOXMDFEV+x3H4O84G4h6ptaFZ+l3QXsC1kB6RNdn8/dI+RoWqglK3hQgLfcSkT4lMDSpdIHoMnpED+xmn0uD13Yslt+/tWl4xD0YRVPlYxFMSsdok7ErCYg7/jkqpquxGpbK1jVgnXoBptDKF4zINEZmWzYFnoSh8LNMub0Gig9XobqgKeTtDGPCDQQeygJGMgwQaGiQBGUmP1qY6nk5b1ZLLrrtKoYipHFsAI2SJ"


    val LOGTAG: String = ""

    // Reference to current Activity
    private var activity: Activity = arAppControl as Activity

    //Flags
    var isStarted = false
        private set
    var isCameraRunning = false
        private set

    var dataSetUserDef: DataSet? = null


    // The async tasks to initialize the Vuforia SDK:
    private var initVuforiaTask: InitVuforiaTask? = null
    private var initTrackerTask: InitTrackerTask? = null
    private var loadTrackerTask: LoadTrackerTask? = null
    private var startVuforiaTask: StartVuforiaTask? = null
    private var resumeVuforiaTask: ResumeVuforiaTask? = null

    // An object used for synchronizing Vuforia initialization, data set loading
    // and the Android onDestroy() life cycle event. If the application is
    // destroyed while a data set is still being loaded, then we wait for the
    // loading operation to finish before shutting down Vuforia:
    private val lifecycleLock = Any()

    // Vuforia initialization flags
    private var vuforiaFlags = 0

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

        // Set vuforia flags
        vuforiaFlags = INIT_FLAGS.GL_20
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
                Vuforia.registerCallback(this@ARApplicationSession)
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
        Vuforia.setInitParameters(activity, vuforiaFlags, VUFORIA_KEY)

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
        }

        if (!objectTracker.activateDataSet(dataSetUserDef)) {
            Log.d(LOGTAG, "Failed to activate data set.")
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
                //objectTracker.stop()
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


            Log.d(LOGTAG, "Attempting to transfer the trackable source to the dataset")

            // Deactivate current dataset
            objectTracker.deactivateDataSet(objectTracker.getActiveDataSet(0))

            // Clear the oldest target if the dataset is full or the dataset
            // already contains five user-defined targets.
            if (dataSetUserDef!!.hasReachedTrackableLimit() || dataSetUserDef!!.numTrackables >= 5)
                dataSetUserDef!!.destroy(dataSetUserDef!!.getTrackable(0))

            if (dataSetUserDef!!.numTrackables > 0) {
                // We need to stop the extended tracking for the previous target
                // so we can enable it for the new one
                val previousCreatedTrackableIndex = dataSetUserDef!!.numTrackables - 1

                objectTracker.resetExtendedTracking()
                dataSetUserDef!!.getTrackable(previousCreatedTrackableIndex).stopExtendedTracking()
            }

            // Add new trackable source
            val trackable = dataSetUserDef!!.createTrackable(objectTracker.imageTargetBuilder.trackableSource)

            // Reactivate current dataset
            objectTracker.activateDataSet(dataSetUserDef)

//            if (mExtendedTracking) {
//                trackable.startExtendedTracking()
//            }


    }














    fun startAR(camera: Int) {

        var vuforiaException: ARApplicationException? = null
        cameraConfig = camera

        try {
            startVuforiaTask = StartVuforiaTask()
            startVuforiaTask?.execute()
        } catch (e: Exception) {
            val logMessage = "Starting Vuforia failed"
            vuforiaException = ARApplicationException(ARApplicationException.CAMERA_INITIALIZATION_FAILURE, logMessage)
            Log.e(LOGTAG, logMessage)
        }

        if (vuforiaException != null) {
            // Send Vuforia Exception to the application and call initDone
            // to stop initialization process
            arAppControl.onInitARDone(vuforiaException)
        }
    }

    // Stops any ongoing initialization, stops Vuforia
    @Throws(ARApplicationException::class)
    fun stopAR() {
        // Cancel potentially running tasks
        if (initVuforiaTask != null && initVuforiaTask?.status != AsyncTask.Status.FINISHED) {
            initVuforiaTask?.cancel(true)
            initVuforiaTask = null
        }

        if (loadTrackerTask != null && loadTrackerTask?.status != AsyncTask.Status.FINISHED) {
            loadTrackerTask?.cancel(true)
            loadTrackerTask = null
        }

        initVuforiaTask = null
        loadTrackerTask = null

        isStarted = false

        stopCamera()

        // Ensure that all asynchronous operations to initialize Vuforia
        // and loading the tracker datasets do not overlap:
        synchronized(lifecycleLock) {

            val unloadTrackersResult: Boolean
            val deinitTrackersResult: Boolean

            // Destroy the tracking data set:
            unloadTrackersResult = arAppControl.doUnloadTrackersData()

            // Deinitialize the trackers:
            deinitTrackersResult = arAppControl.doDeinitTrackers()

            // Deinitialize Vuforia SDK:
            Vuforia.deinit()

            if (!unloadTrackersResult)
                throw ARApplicationException(ARApplicationException.UNLOADING_TRACKERS_FAILURE, "Failed to unload trackers\' data")

            if (!deinitTrackersResult)
                throw ARApplicationException(ARApplicationException.TRACKERS_DEINITIALIZATION_FAILURE, "Failed to deinitialize trackers")

        }
    }


    // Resumes Vuforia, restarts the trackers and the camera
    private fun resumeAR() {
        var vuforiaException: ARApplicationException? = null

        try {
            resumeVuforiaTask = ResumeVuforiaTask()
            resumeVuforiaTask?.execute()
        } catch (e: Exception) {
            val logMessage = "Resuming Vuforia failed"
            vuforiaException = ARApplicationException(ARApplicationException.INITIALIZATION_FAILURE, logMessage)
            Log.e(LOGTAG, logMessage)
        }

        if (vuforiaException != null) {
            // Send Vuforia Exception to the application and call initDone
            // to stop initialization process
            arAppControl.onInitARDone(vuforiaException)
        }
    }

    // Pauses Vuforia and stops the camera
    @Throws(ARApplicationException::class)
    fun pauseAR() {
        if (isStarted) {
            stopCamera()
        }

        Vuforia.onPause()
    }

    // Manages the configuration changes
    fun onConfigurationChanged() = Device.getInstance().setConfigurationChanged()


    // Methods to be called to handle lifecycle
    fun onResume() {
        if (resumeVuforiaTask == null || resumeVuforiaTask?.status == AsyncTask.Status.FINISHED) {
            // onResume() will sometimes be called twice depending on the screen lock mode
            // This will prevent redundant AsyncTasks from being executed
            resumeAR()
        }
    }

    // An async task to initialize Vuforia asynchronously.
    private inner class InitVuforiaTask : AsyncTask<Void, Int, Boolean>() {
        // Initialize with invalid value:
        private var progressValue = -1

        override fun doInBackground(vararg params: Void): Boolean? {
            // Prevent the onDestroy() method to overlap with initialization:
            synchronized(lifecycleLock) {
                Vuforia.setInitParameters(activity, vuforiaFlags, "AQQRe6b/////AAAAGcmQz5avqE61gOA0Z/QZxrZWeh2KZN5w64g7UWeagaUwuYUVH863O90Q4QyYCZWC/OHJgA+aOmwZ6HEMktHer59DuPUUzFhipbLLneJf4kjGVssReqak5oN+muwzWRnq0w+uOXMDFEV+x3H4O84G4h6ptaFZ+l3QXsC1kB6RNdn8/dI+RoWqglK3hQgLfcSkT4lMDSpdIHoMnpED+xmn0uD13Yslt+/tWl4xD0YRVPlYxFMSsdok7ErCYg7/jkqpquxGpbK1jVgnXoBptDKF4zINEZmWzYFnoSh8LNMub0Gig9XobqgKeTtDGPCDQQeygJGMgwQaGiQBGUmP1qY6nk5b1ZLLrrtKoYipHFsAI2SJ")

                do {
                    // Vuforia.init() blocks until an initialization step is
                    // complete, then it proceeds to the next step and reports
                    // progress in percents (0 ... 100%).
                    // If Vuforia.init() returns -1, it indicates an error.
                    // Initialization is done when progress has reached 100%.
                    progressValue = Vuforia.init()

                    // Publish the progress value:
                    publishProgress(progressValue)

                    // We check whether the task has been canceled in the
                    // meantime (by calling AsyncTask.cancel(true)).
                    // and bail out if it has, thus stopping this thread.
                    // This is necessary as the AsyncTask will run to completion
                    // regardless of the status of the component that
                    // started is.
                } while (!isCancelled && progressValue >= 0 && progressValue < 100)

                return progressValue > 0
            }
        }

        override fun onProgressUpdate(vararg values: Int?) {

        }

        override fun onPostExecute(result: Boolean?) {
            // Done initializing Vuforia, proceed to next application
            // initialization status
            Log.d(LOGTAG, "InitVuforiaTask.onPostExecute: execution ${if (result!!) "successful" else "failed"}")

            var vuforiaException: ARApplicationException? = null

            if (result) {
                try {
                    initTrackerTask = InitTrackerTask()
                    initTrackerTask?.execute()
                } catch (e: Exception) {
                    val logMessage = "Failed to initialize tracker."
                    vuforiaException = ARApplicationException(ARApplicationException.TRACKERS_INITIALIZATION_FAILURE, logMessage)
                    Log.e(LOGTAG, logMessage)
                }

            } else {
                // NOTE: Check if initialization failed because the device is
                // not supported. At this point the user should be informed
                // with a message.
                val logMessage: String = getInitializationErrorString(progressValue)

                // Log error:
                Log.e(LOGTAG, "InitVuforiaTask.onPostExecute: $logMessage Exiting.")

                vuforiaException = ARApplicationException(ARApplicationException.INITIALIZATION_FAILURE, logMessage)
            }

            if (vuforiaException != null) {
                // Send Vuforia Exception to the application and call initDone
                // to stop initialization process
                arAppControl.onInitARDone(vuforiaException)
            }
        }
    }

    // An async task to resume Vuforia asynchronously
    private inner class ResumeVuforiaTask : AsyncTask<Void, Void, Void>() {
        override fun doInBackground(vararg params: Void): Void? {
            // Prevent the concurrent lifecycle operations:
            synchronized(lifecycleLock) {
                Vuforia.onResume()
            }

            return null
        }

        override fun onPostExecute(result: Void) {
            Log.d(LOGTAG, "ResumeVuforiaTask.onPostExecute")

            // We may start the camera only if the Vuforia SDK has already been initialized
            if (isStarted && !isCameraRunning) {
                startAR(cameraConfig)
                arAppControl.onVuforiaResumed()
            }
        }
    }

    // An async task to initialize trackers asynchronously
    private inner class InitTrackerTask : AsyncTask<Void, Int, Boolean>() {
        override fun doInBackground(vararg params: Void): Boolean? {
            synchronized(lifecycleLock) {
                // Load the tracker data set:
                return arAppControl.doInitTrackers()
            }
        }

        override fun onPostExecute(result: Boolean?) {
            Log.d(LOGTAG, "InitTrackerTask.onPostExecute: execution ${if (result!!) "successful" else "failed"}")

            var vuforiaException: ARApplicationException? = null

            if (result) {
                try {
                    loadTrackerTask = LoadTrackerTask()
                    loadTrackerTask?.execute()
                } catch (e: Exception) {
                    val logMessage = "Failed to load tracker data."
                    Log.e(LOGTAG, logMessage)
                    vuforiaException = ARApplicationException(ARApplicationException.LOADING_TRACKERS_FAILURE, logMessage)
                }

            } else {
                val logMessage = "Failed to load tracker data."
                Log.e(LOGTAG, logMessage)

                // Error loading dataset
                vuforiaException = ARApplicationException(ARApplicationException.TRACKERS_INITIALIZATION_FAILURE, logMessage)
            }

            if (vuforiaException != null) {
                // Send Vuforia Exception to the application and call initDone
                // to stop initialization process
                arAppControl.onInitARDone(vuforiaException)
            }
        }
    }

    // An async task to load the tracker data asynchronously.
    private inner class LoadTrackerTask : AsyncTask<Void, Void, Boolean>() {
        override fun doInBackground(vararg params: Void): Boolean? {
            // Prevent the concurrent lifecycle operations:
            synchronized(lifecycleLock) {
                // Load the tracker data set:
                return arAppControl.doLoadTrackersData()
            }
        }

        override fun onPostExecute(result: Boolean?) {
            var vuforiaException: ARApplicationException? = null

            Log.d(LOGTAG, "LoadTrackerTask.onPostExecute: execution ${if (result!!) "successful" else "failed"}")

            if (result) {
                // Hint to the virtual machine that it would be a good time to
                // run the garbage collector:
                //
                // NOTE: This is only a hint. There is no guarantee that the
                // garbage collector will actually be run.
                System.gc()

                Vuforia.registerCallback(this@ARApplicationSession)

                isStarted = true
            } else {
                val logMessage = "Failed to load tracker data."
                // Error loading dataset
                Log.e(LOGTAG, logMessage)
                vuforiaException = ARApplicationException(ARApplicationException.LOADING_TRACKERS_FAILURE, logMessage)
            }

            // Done loading the tracker, update application status, send the
            // exception to check errors
            arAppControl.onInitARDone(vuforiaException)
        }
    }

    // An async task to start the camera and trackers
    private inner class StartVuforiaTask : AsyncTask<Void, Void, Boolean>() {
        var vuforiaException: ARApplicationException? = null

        override fun doInBackground(vararg params: Void): Boolean? {
            // Prevent the concurrent lifecycle operations:
            synchronized(lifecycleLock) {
                try {
                    startCamera(cameraConfig)
                } catch (e: ARApplicationException) {
                    Log.e(LOGTAG, "StartVuforiaTask.doInBackground: Could not start AR with exception: " + e)
                    vuforiaException = e
                }
            }

            return true
        }

        override fun onPostExecute(result: Boolean?) {
            Log.d(LOGTAG, "StartVuforiaTask.onPostExecute: execution ${if (result!!) "successful" else "failed"}")

            arAppControl.onVuforiaStarted()

            if (vuforiaException != null) {
                // Send Vuforia Exception to the application and call initDone
                // to stop initialization process
                arAppControl.onInitARDone(vuforiaException!!)
            }
        }
    }


    // Returns the error message for each error code
    private fun getInitializationErrorString(code: Int): String = when (code) {
        INIT_ERRORCODE.INIT_DEVICE_NOT_SUPPORTED -> activity.getString(R.string.INIT_ERROR_DEVICE_NOT_SUPPORTED)
        INIT_ERRORCODE.INIT_NO_CAMERA_ACCESS -> activity.getString(R.string.INIT_ERROR_NO_CAMERA_ACCESS)
        INIT_ERRORCODE.INIT_LICENSE_ERROR_MISSING_KEY -> activity.getString(R.string.INIT_LICENSE_ERROR_MISSING_KEY)
        INIT_ERRORCODE.INIT_LICENSE_ERROR_INVALID_KEY -> activity.getString(R.string.INIT_LICENSE_ERROR_INVALID_KEY)
        INIT_ERRORCODE.INIT_LICENSE_ERROR_NO_NETWORK_TRANSIENT -> activity.getString(R.string.INIT_LICENSE_ERROR_NO_NETWORK_TRANSIENT)
        INIT_ERRORCODE.INIT_LICENSE_ERROR_NO_NETWORK_PERMANENT -> activity.getString(R.string.INIT_LICENSE_ERROR_NO_NETWORK_PERMANENT)
        INIT_ERRORCODE.INIT_LICENSE_ERROR_CANCELED_KEY -> activity.getString(R.string.INIT_LICENSE_ERROR_CANCELED_KEY)
        INIT_ERRORCODE.INIT_LICENSE_ERROR_PRODUCT_TYPE_MISMATCH -> activity.getString(R.string.INIT_LICENSE_ERROR_PRODUCT_TYPE_MISMATCH)
        else -> activity.getString(R.string.INIT_LICENSE_ERROR_UNKNOWN_ERROR)
    }



}