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
import io.reactivex.Observable


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

    fun startVuforia(): Completable {
        return Completable.create { emitter ->
            try {
                Vuforia.setInitParameters(activity, vuforiaFlags, VUFORIA_KEY)

                var progressValue: Int
                do {
                    // Vuforia.init() blocks until an initialization step is
                    // complete, then it proceeds to the next step and reports
                    // progress in percents (0 ... 100%).
                    // If Vuforia.init() returns -1, it indicates an error.
                    // Initialization is done when progress has reached 100%.
                    progressValue = Vuforia.init()
                } while (!emitter.isDisposed && progressValue in 0..99)

                startCameraAndTrackers(cameraConfig)

                isStarted = true

                emitter.onComplete()
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }
    }

    fun startVuforiaARCamera(): Observable<Boolean> {
        return Observable.create<Boolean> { emitter ->
            try {
                synchronized(lifecycleLock) {
                    startCameraAndTrackers(cameraConfig)
                }
                emitter.onComplete()
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }
    }

    fun resumeVuforia(): Completable {
        return Completable.create { emitter ->
            try {
                // Prevent the concurrent lifecycle operations:
                synchronized(lifecycleLock) {
                    Vuforia.onResume()
                    // We may start the camera only if the Vuforia SDK has already been initialized
                    if (isStarted && !isCameraRunning) {
                        startCameraAndTrackers(cameraConfig)
                    }
                }
                emitter.onComplete()
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }
    }

    fun pauseVuforia(): Completable {
        return Completable.create { emitter ->
            try {
                if (isStarted) {
                    stopCamera()
                }
                Vuforia.onPause()
                emitter.onComplete()
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }
    }











    fun initAR(activity: Activity, screenOrientation: Int) {
        var vuforiaException: ARApplicationException? = null
        this.activity = activity

        var orientation = screenOrientation
        if (screenOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR && Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
            orientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
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
        activity.requestedOrientation = orientation

        // As long as this window is visible to the user, keep the device's screen turned on and bright
        activity.window.setFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON, WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

        // Set vuforia flags
        vuforiaFlags = INIT_FLAGS.GL_20

        // Initialize Vuforia SDK asynchronously to avoid blocking the
        // main (UI) thread.
        //
        // NOTE: This task instance must be created and invoked on the
        // UI thread and it can be executed only once!
        if (initVuforiaTask != null) {
            val logMessage = "Cannot initialize SDK twice"
            vuforiaException = ARApplicationException(ARApplicationException.VUFORIA_ALREADY_INITIALIZATED, logMessage)
            Log.e(LOGTAG, logMessage)
        }

        if (vuforiaException == null) {
            try {
                initVuforiaTask = InitVuforiaTask()
                initVuforiaTask?.execute()
            } catch (e: Exception) {
                val logMessage = "Initializing Vuforia SDK failed"
                vuforiaException = ARApplicationException(ARApplicationException.INITIALIZATION_FAILURE, logMessage)
                Log.e(LOGTAG, logMessage)
            }

        }

        if (vuforiaException != null) {
            // Send Vuforia Exception to the application and call initDone
            // to stop initialization process
            //arAppControl.onInitARDone(vuforiaException)
        }
    }

    fun onSurfaceCreated() {
        Vuforia.onSurfaceCreated()
    }

    fun onSurfaceChanged(width: Int, height: Int) {
        Vuforia.onSurfaceChanged(width, height)
    }

    // Starts Vuforia, initialize and starts the camera and start the trackers
    @Throws(ARApplicationException::class)
    private fun startCameraAndTrackers(camera: Int) {
        val error: String
        if (isCameraRunning) {
            error = "Camera already running, unable to open again"
            Log.e(LOGTAG, error)
            throw ARApplicationException(ARApplicationException.CAMERA_INITIALIZATION_FAILURE, error)
        }

        cameraConfig = camera
        if (!CameraDevice.getInstance().init(camera)) {
            error = "Unable to open camera device: " + camera
            Log.e(LOGTAG, error)
            throw ARApplicationException(ARApplicationException.CAMERA_INITIALIZATION_FAILURE, error)
        }

        if (!CameraDevice.getInstance().selectVideoMode(CameraDevice.MODE.MODE_DEFAULT)) {
            error = "Unable to set video mode"
            Log.e(LOGTAG, error)
            throw ARApplicationException(ARApplicationException.CAMERA_INITIALIZATION_FAILURE, error)
        }

        if (!CameraDevice.getInstance().start()) {
            error = "Unable to start camera device: " + camera
            Log.e(LOGTAG, error)
            throw ARApplicationException(ARApplicationException.CAMERA_INITIALIZATION_FAILURE, error)
        }

//        arAppControl.doStartTrackers()

        isCameraRunning = true
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

    override fun Vuforia_onUpdate(p0: State?) {
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
                    startCameraAndTrackers(cameraConfig)
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

    fun stopCamera() {
        if (isCameraRunning) {
            arAppControl.doStopTrackers()
            isCameraRunning = false
            CameraDevice.getInstance().stop()
            CameraDevice.getInstance().deinit()
        }
    }

}