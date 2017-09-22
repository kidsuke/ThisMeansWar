package com.datpug.ar

import android.opengl.GLES20
import android.util.Log

/**
 * Created by longvu on 22/09/2017.
 */
object SampleUtils {

    private val LOGTAG = "SampleUtils"


    internal fun initShader(shaderType: Int, source: String): Int {
        var shader = GLES20.glCreateShader(shaderType)
        if (shader != 0) {
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)

            val glStatusVar = intArrayOf(GLES20.GL_FALSE)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, glStatusVar,
                    0)
            if (glStatusVar[0] == GLES20.GL_FALSE) {
                Log.e(LOGTAG, "Could NOT compile shader " + shaderType + " : "
                        + GLES20.glGetShaderInfoLog(shader))
                GLES20.glDeleteShader(shader)
                shader = 0
            }

        }

        return shader
    }


    fun createProgramFromShaderSrc(vertexShaderSrc: String,
                                   fragmentShaderSrc: String): Int {
        val vertShader = initShader(GLES20.GL_VERTEX_SHADER, vertexShaderSrc)
        val fragShader = initShader(GLES20.GL_FRAGMENT_SHADER,
                fragmentShaderSrc)

        if (vertShader == 0 || fragShader == 0)
            return 0

        var program = GLES20.glCreateProgram()
        if (program != 0) {
            GLES20.glAttachShader(program, vertShader)
            checkGLError("glAttchShader(vert)")

            GLES20.glAttachShader(program, fragShader)
            checkGLError("glAttchShader(frag)")

            GLES20.glLinkProgram(program)
            val glStatusVar = intArrayOf(GLES20.GL_FALSE)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, glStatusVar,
                    0)
            if (glStatusVar[0] == GLES20.GL_FALSE) {
                Log.e(
                        LOGTAG,
                        "Could NOT link program : " + GLES20.glGetProgramInfoLog(program))
                GLES20.glDeleteProgram(program)
                program = 0
            }
        }

        return program
    }


    fun checkGLError(op: String) {
        var error = GLES20.glGetError()
        while (error != 0) {
            Log.e(
                    LOGTAG,
                    "After operation " + op + " got glError 0x"
                            + Integer.toHexString(error))
            error = GLES20
                    .glGetError()
        }
    }


    // Transforms a screen pixel to a pixel onto the camera image,
    // taking into account e.g. cropping of camera image to fit different aspect
    // ratio screen.
    // for the camera dimensions, the width is always bigger than the height
    // (always landscape orientation)
    // Top left of screen/camera is origin
    fun screenCoordToCameraCoord(screenX: Int, screenY: Int,
                                 screenDX: Int, screenDY: Int, screenWidth: Int, screenHeight: Int,
                                 cameraWidth: Int, cameraHeight: Int, cameraX: IntArray?, cameraY: IntArray?,
                                 cameraDX: IntArray?, cameraDY: IntArray?, displayRotation: Int, cameraRotation: Int) {
        var screenX = screenX
        var screenY = screenY
        var screenDX = screenDX
        var screenDY = screenDY
        var screenWidth = screenWidth
        var screenHeight = screenHeight
        val videoWidth: Float
        val videoHeight: Float
        videoWidth = cameraWidth.toFloat()
        videoHeight = cameraHeight.toFloat()

        // Compute the angle by which the camera image should be rotated clockwise so that it is
        // shown correctly on the display given its current orientation.
        val correctedRotation = (displayRotation * 90 - cameraRotation + 360) % 360 / 90

        when (correctedRotation) {
            0 -> { }
            1 -> {
                var tmp = screenX
                screenX = screenHeight - screenY
                screenY = tmp

                tmp = screenDX
                screenDX = screenDY
                screenDY = tmp

                tmp = screenWidth
                screenWidth = screenHeight
                screenHeight = tmp
            }

            2 -> {
                screenX = screenWidth - screenX
                screenY = screenHeight - screenY
            }

            3 -> {

                var tmp = screenX
                screenX = screenY
                screenY = screenWidth - tmp

                tmp = screenDX
                screenDX = screenDY
                screenDY = tmp

                tmp = screenWidth
                screenWidth = screenHeight
                screenHeight = tmp
            }
        }

        val videoAspectRatio = videoHeight / videoWidth
        val screenAspectRatio = screenHeight.toFloat() / screenWidth.toFloat()

        val scaledUpX: Float
        val scaledUpY: Float
        val scaledUpVideoWidth: Float
        val scaledUpVideoHeight: Float

        if (videoAspectRatio < screenAspectRatio) {
            // the video height will fit in the screen height
            scaledUpVideoWidth = screenHeight.toFloat() / videoAspectRatio
            scaledUpVideoHeight = screenHeight.toFloat()
            scaledUpX = screenX.toFloat() + (scaledUpVideoWidth - screenWidth.toFloat()) / 2.0f
            scaledUpY = screenY.toFloat()
        } else {
            // the video width will fit in the screen width
            scaledUpVideoHeight = screenWidth.toFloat() * videoAspectRatio
            scaledUpVideoWidth = screenWidth.toFloat()
            scaledUpY = screenY.toFloat() + (scaledUpVideoHeight - screenHeight.toFloat()) / 2.0f
            scaledUpX = screenX.toFloat()
        }

        if (cameraX != null && cameraX.size > 0) {
            cameraX[0] = (scaledUpX / scaledUpVideoWidth.toFloat() * videoWidth).toInt()
        }

        if (cameraY != null && cameraY.size > 0) {
            cameraY[0] = (scaledUpY / scaledUpVideoHeight.toFloat() * videoHeight).toInt()
        }

        if (cameraDX != null && cameraDX.size > 0) {
            cameraDX[0] = (screenDX.toFloat() / scaledUpVideoWidth.toFloat() * videoWidth).toInt()
        }

        if (cameraDY != null && cameraDY.size > 0) {
            cameraDY[0] = (screenDY.toFloat() / scaledUpVideoHeight.toFloat() * videoHeight).toInt()
        }
    }


    fun getOrthoMatrix(nLeft: Float, nRight: Float,
                       nBottom: Float, nTop: Float, nNear: Float, nFar: Float): FloatArray {
        val nProjMatrix = FloatArray(16)

        var i: Int
        i = 0
        while (i < 16) {
            nProjMatrix[i] = 0.0f
            i++
        }

        nProjMatrix[0] = 2.0f / (nRight - nLeft)
        nProjMatrix[5] = 2.0f / (nTop - nBottom)
        nProjMatrix[10] = 2.0f / (nNear - nFar)
        nProjMatrix[12] = -(nRight + nLeft) / (nRight - nLeft)
        nProjMatrix[13] = -(nTop + nBottom) / (nTop - nBottom)
        nProjMatrix[14] = (nFar + nNear) / (nFar - nNear)
        nProjMatrix[15] = 1.0f

        return nProjMatrix
    }

}