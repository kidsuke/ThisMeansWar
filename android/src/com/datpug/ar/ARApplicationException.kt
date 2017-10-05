package com.datpug.ar

/**
 * Created by longvu on 20/09/2017.
 */
class ARApplicationException(val code: Int, description: String): Exception(description) {

    companion object {
        private val serialVersionUID = 2L

        val INITIALIZATION_FAILURE = 0
        val VUFORIA_ALREADY_INITIALIZATED = 1
        val TRACKERS_INITIALIZATION_FAILURE = 2
        val LOADING_TRACKERS_FAILURE = 3
        val UNLOADING_TRACKERS_FAILURE = 4
        val TRACKERS_DEINITIALIZATION_FAILURE = 5
        val CAMERA_INITIALIZATION_FAILURE = 6
        val SET_FOCUS_MODE_FAILURE = 7
        val ACTIVATE_FLASH_FAILURE = 8
    }

}