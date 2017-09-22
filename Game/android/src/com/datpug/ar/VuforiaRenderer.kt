package com.datpug.ar

import android.app.Activity
import android.util.Log
import android.opengl.GLES20
import android.opengl.Matrix
import com.datpug.ARRenderer
import com.vuforia.Vuforia.onSurfaceChanged
import com.vuforia.RendererHelper.drawVideoBackground
import com.vuforia.*


/**
 * Created by longvu on 21/09/2017.
 */
class VuforiaRenderer(val arAppSession: ARApplicationSession, val deviceMode: Int, val stereo: Boolean): ARRenderer {
    private var renderer: Renderer? = null

    private var mRenderingPrimitives: RenderingPrimitives? = null
    private var currentView = VIEW.VIEW_SINGULAR
    private var mNearPlane = -1.0f
    private var mFarPlane = -1.0f

    private var videoBackgroundTex: GLTextureUnit? = null

    // Shader user to render the video background on AR mode
    private var vbShaderProgramID = 0
    private var vbTexSampler2DHandle = 0
    private var vbVertexHandle = 0
    private var vbTexCoordHandle = 0
    private var vbProjectionMatrixHandle = 0

    override fun initRendering(screenWidth: Int, screenHeight: Int) {
        if (deviceMode != Device.MODE.MODE_AR && deviceMode != Device.MODE.MODE_VR) {
            Log.e("", "Device mode should be Device.MODE.MODE_AR or Device.MODE.MODE_VR")
            throw IllegalArgumentException()
        }
        Device.getInstance().apply {
            isViewerActive = stereo
            mode = deviceMode
        }

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, if (Vuforia.requiresAlpha()) 0.0f else 1.0f)

        renderer = Renderer.getInstance()
        onSurfaceCreated()
        configureVideoBackground(screenWidth, screenHeight, true)
    }

    override fun resize(width: Int, height: Int) {
        onSurfaceChanged(width, height)
    }

    // Called when the surface changed size.
    fun onSurfaceChanged(width: Int, height: Int) {
        // Call Vuforia function to handle render surface size changes:
        arAppSession.onSurfaceChanged(width, height)
    }

    fun onSurfaceCreated() {
        arAppSession.onSurfaceCreated()

        vbShaderProgramID = SampleUtils.createProgramFromShaderSrc(VideoBackgroundShader.VB_VERTEX_SHADER,
                VideoBackgroundShader.VB_FRAGMENT_SHADER)

        // Rendering configuration for video background
        if (vbShaderProgramID > 0) {
            // Activate shader:
            GLES20.glUseProgram(vbShaderProgramID)

            // Retrieve handler for texture sampler shader uniform variable:
            vbTexSampler2DHandle = GLES20.glGetUniformLocation(vbShaderProgramID, "texSampler2D")

            // Retrieve handler for projection matrix shader uniform variable:
            vbProjectionMatrixHandle = GLES20.glGetUniformLocation(vbShaderProgramID, "projectionMatrix")

            vbVertexHandle = GLES20.glGetAttribLocation(vbShaderProgramID, "vertexPosition")
            vbTexCoordHandle = GLES20.glGetAttribLocation(vbShaderProgramID, "vertexTexCoord")
            vbProjectionMatrixHandle = GLES20.glGetUniformLocation(vbShaderProgramID, "projectionMatrix")
            vbTexSampler2DHandle = GLES20.glGetUniformLocation(vbShaderProgramID, "texSampler2D")

            // Stop using the program
            GLES20.glUseProgram(0)
        }

        videoBackgroundTex = GLTextureUnit()
    }

    // The render function.
    override fun render() {
//        if (!mIsActive)
//            return null

        val state = renderer?.begin()
        renderVideoBackground()
        renderer?.end()


        // did we find any trackables this frame?
//        val results = arrayOfNulls<TrackableResult>(state.getNumTrackableResults())
//        for (tIdx in 0..state.getNumTrackableResults() - 1) {
//            //remember trackable
//            val result = state.getTrackableResult(tIdx)
//            lastTrackableName = result.getTrackable().getName()
//            results[tIdx] = result
//
//            //calculate filed of view
//            val calibration = CameraDevice.getInstance().getCameraCalibration()
//            val size = calibration.getSize()
//            val focalLength = calibration.getFocalLength()
//            fieldOfViewRadians = (2 * Math.atan((0.5f * size.getData()[0] / focalLength.getData()[0]).toDouble())).toFloat()
//        }

    }

    // Configures the video mode and sets offsets for the camera's image
    private fun configureVideoBackground(screenWidth: Int, screenHeight: Int, isPortrait: Boolean) {
        val cameraDevice = CameraDevice.getInstance()
        val vm = cameraDevice.getVideoMode(CameraDevice.MODE.MODE_DEFAULT)

        val config = VideoBackgroundConfig()
        config.enabled = true
        config.position = Vec2I(0, 0)

        var xSize = 0
        var ySize = 0

        // We keep the aspect ratio to keep the video correctly rendered. If it is portrait we
        // preserve the height and scale width and vice versa if it is landscape, we preserve
        // the width and we check if the selected values fill the screen, otherwise we invert
        // the selection
        if (isPortrait) {
            xSize = (vm.height * (screenHeight / vm.width.toFloat())).toInt()
            ySize = screenHeight

            if (xSize < screenWidth) {
                xSize = screenWidth
                ySize = (screenWidth * (vm.width / vm.height.toFloat())).toInt()
            }
        } else {
            xSize = screenWidth
            ySize = (vm.height * (screenWidth / vm.width.toFloat())).toInt()

            if (ySize < screenHeight) {
                xSize = (screenHeight * (vm.width / vm.height.toFloat())).toInt()
                ySize = screenHeight
            }
        }

        config.size = Vec2I(xSize, ySize)

        Log.i("", "Configure Video Background : Video (" + vm.width
                + " , " + vm.height + "), Screen (" + screenWidth + " , "
                + screenHeight + "), mSize (" + xSize + " , " + ySize + ")")

        Renderer.getInstance().videoBackgroundConfig = config
    }

    private fun renderVideoBackground() {
        mRenderingPrimitives = Device.getInstance().renderingPrimitives

        if (currentView == VIEW.VIEW_POSTPROCESS)
            return

        val vbVideoTextureUnit = 0
        // Bind the video bg texture and get the Texture ID from Vuforia
        videoBackgroundTex!!.textureUnit = vbVideoTextureUnit
        if (!renderer!!.updateVideoBackgroundTexture(videoBackgroundTex)) {
            Log.e("", "Unable to update video background texture")
            return
        }

        val vbProjectionMatrix = Tool.convert2GLMatrix(
                mRenderingPrimitives!!.getVideoBackgroundProjectionMatrix(currentView, COORDINATE_SYSTEM_TYPE.COORDINATE_SYSTEM_CAMERA)).data

        // Apply the scene scale on video see-through eyewear, to scale the video background and augmentation
        // so that the display lines up with the real world
        // This should not be applied on optical see-through devices, as there is no video background,
        // and the calibration ensures that the augmentation matches the real world
        if (Device.getInstance().isViewerActive) {
            val sceneScaleFactor = getSceneScaleFactor().toFloat()
            Matrix.scaleM(vbProjectionMatrix, 0, sceneScaleFactor, sceneScaleFactor, 1.0f)
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST)

        val vbMesh = mRenderingPrimitives!!.getVideoBackgroundMesh(currentView)
        // Load the shader and upload the vertex/texcoord/index data
        GLES20.glUseProgram(vbShaderProgramID)
        GLES20.glVertexAttribPointer(vbVertexHandle, 3, GLES20.GL_FLOAT, false, 0, vbMesh.positions.asFloatBuffer())
        GLES20.glVertexAttribPointer(vbTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, vbMesh.uVs.asFloatBuffer())

        GLES20.glUniform1i(vbTexSampler2DHandle, vbVideoTextureUnit)

        // Render the video background with the custom shader
        // First, we enable the vertex arrays
        GLES20.glEnableVertexAttribArray(vbVertexHandle)
        GLES20.glEnableVertexAttribArray(vbTexCoordHandle)

        // Pass the projection matrix to OpenGL
        GLES20.glUniformMatrix4fv(vbProjectionMatrixHandle, 1, false, vbProjectionMatrix, 0)

        // Then, we issue the render call
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, vbMesh.numTriangles * 3, GLES20.GL_UNSIGNED_SHORT,
                vbMesh.triangles.asShortBuffer())

        // Finally, we disable the vertex arrays
        GLES20.glDisableVertexAttribArray(vbVertexHandle)
        GLES20.glDisableVertexAttribArray(vbTexCoordHandle)
    }

    val VIRTUAL_FOV_Y_DEGS = 85.0f
    val M_PI = 3.14159f

    private fun getSceneScaleFactor(): Double {
        // Get the y-dimension of the physical camera field of view
        val fovVector = CameraDevice.getInstance().cameraCalibration.fieldOfViewRads
        val cameraFovYRads = fovVector.data[1]

        // Get the y-dimension of the virtual camera field of view
        val virtualFovYRads = VIRTUAL_FOV_Y_DEGS * M_PI / 180

        // The scene-scale factor represents the proportion of the viewport that is filled by
        // the video background when projected onto the same plane.
        // In order to calculate this, let 'd' be the distance between the cameras and the plane.
        // The height of the projected image 'h' on this plane can then be calculated:
        //   tan(fov/2) = h/2d
        // which rearranges to:
        //   2d = h/tan(fov/2)
        // Since 'd' is the same for both cameras, we can combine the equations for the two cameras:
        //   hPhysical/tan(fovPhysical/2) = hVirtual/tan(fovVirtual/2)
        // Which rearranges to:
        //   hPhysical/hVirtual = tan(fovPhysical/2)/tan(fovVirtual/2)
        // ... which is the scene-scale factor
        return Math.tan((cameraFovYRads / 2).toDouble()) / Math.tan((virtualFovYRads / 2).toDouble())
    }
}