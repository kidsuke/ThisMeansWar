package com.datpug

/**
 * Created by longvu on 22/09/2017.
 */
abstract class ARRenderer {
    protected var renderListeners: List<OnARRenderListener> = listOf()

    abstract fun initRendering(screenWidth: Int, screenHeight: Int)
    abstract fun setRendererActive(active: Boolean)
    abstract fun resize(width: Int, height: Int)
    abstract fun render()

    fun addOnARRenderListener(listener: OnARRenderListener) {
        renderListeners = renderListeners.plus(listener)
    }

    fun removeOnARRenderListener(listener: OnARRenderListener) {
        renderListeners = renderListeners.filterNot { it === listener }
    }

    interface OnARRenderListener {
        fun onRender(data: Array<FloatArray>)
    }
}