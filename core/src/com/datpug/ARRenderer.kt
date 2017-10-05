package com.datpug

/**
 * Created by longvu on 22/09/2017.
 */
abstract class ARRenderer {
    protected var arDetectListeners: List<OnARDetectListener> = listOf()

    abstract fun initRendering(screenWidth: Int, screenHeight: Int)
    abstract fun setRendererActive(active: Boolean)
    abstract fun resize(width: Int, height: Int)
    abstract fun render()
    abstract fun removeAR(id: Int)

    fun addOnARRenderListener(listener: OnARDetectListener) {
        arDetectListeners = arDetectListeners.plus(listener)
    }

    fun removeOnARRenderListener(listener: OnARDetectListener) {
        arDetectListeners = arDetectListeners.filterNot { it === listener }
    }

    interface OnARDetectListener {
        fun onARDetected(id: Int, cameraProjection: FloatArray, modelViewProjection: FloatArray)
    }
}