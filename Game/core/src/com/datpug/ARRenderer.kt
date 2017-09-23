package com.datpug

/**
 * Created by longvu on 22/09/2017.
 */
abstract class ARRenderer {
    abstract fun initRendering(screenWidth: Int, screenHeight: Int)
    abstract fun setRendererActive(active: Boolean)
    abstract fun resize(width: Int, height: Int)
    abstract fun render()
}