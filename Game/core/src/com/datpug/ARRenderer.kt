package com.datpug

/**
 * Created by longvu on 22/09/2017.
 */
interface ARRenderer {
    fun initRendering(screenWidth: Int, screenHeight: Int)
    fun resize(width: Int, height: Int)
    fun render()
}