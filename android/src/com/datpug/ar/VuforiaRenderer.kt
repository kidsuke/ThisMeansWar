package com.datpug.ar

import android.util.Log
import android.opengl.GLES20
import android.opengl.Matrix
import com.vuforia.*
import com.vuforia.Matrix44F


/**
 * Created by longvu on 21/09/2017.
 */
class VuforiaRenderer(val arAppSession: VuforiaSession, val deviceMode: Int, val stereo: Boolean): ARRenderer() {
    private lateinit var renderer: Renderer

    private var mRenderingPrimitives: RenderingPrimitives? = null
    private var currentView = VIEW.VIEW_SINGULAR
    private var mNearPlane = 1f
    private var mFarPlane = 5000f

    private var videoBackgroundTex: GLTextureUnit? = null

    // Shader user to render the video background on AR mode
    private var vbShaderProgramID = 0
    private var vbTexSampler2DHandle = 0
    private var vbVertexHandle = 0
    private var vbTexCoordHandle = 0
    private var vbProjectionMatrixHandle = 0

    var screenWidth: Int? = null
        private set
    var screenHeight: Int? = null
        private set
    var isPortrait: Boolean? = true
        private set

    private var isActive = false

    /**
     * This function is used to initialize elements needed for rendering
     * @param screenWidth
     * @param screenHeight
     */
    override fun initRendering(screenWidth: Int, screenHeight: Int) {
        this.screenWidth = screenWidth
        this.screenHeight = screenHeight
        this.renderer = Renderer.getInstance()

        Vuforia.onSurfaceCreated()

        if (deviceMode != Device.MODE.MODE_AR && deviceMode != Device.MODE.MODE_VR) {
            Log.e("", "Device mode should be Device.MODE.MODE_AR or Device.MODE.MODE_VR")
            //throw IllegalArgumentException()
        }
        Device.getInstance().apply {
            isViewerActive = stereo
            mode = deviceMode
        }

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, if (Vuforia.requiresAlpha()) 0.0f else 1.0f)
        vbShaderProgramID = SampleUtils.createProgramFromShaderSrc(VideoBackgroundShader.VB_VERTEX_SHADER, VideoBackgroundShader.VB_FRAGMENT_SHADER)
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

    /**
     * This function is used to decide whether the renderer should be active to render stuff. Otherwise, nothing will be rendered
     * @param active
     */
    override fun setRendererActive(active: Boolean) {
        isActive = active
        // If renderer is activated, configure video background for the camera
        if (isActive) configureVideoBackground()
    }

    /**
     * This function is used when the screen is resize for some reasons
     * @param width
     * @param height
     */
    override fun resize(width: Int, height: Int) {
        Vuforia.onSurfaceChanged(width, height)
    }

    // The render function.
    override fun render() {
        if (!isActive) return

        // Get our current state
        val state: State = TrackerManager.getInstance().stateUpdater.updateState()
        renderer.begin(state)
        renderVideoBackground()

        // We must detect if background reflection is active and adjust the
        // culling direction.
        // If the reflection is active, this means the post matrix has been
        // reflected as well,
        // therefore standard counter clockwise face culling will result in
        // "inside out" models.
        if (Renderer.getInstance().videoBackgroundConfig.reflection == VIDEO_BACKGROUND_REFLECTION.VIDEO_BACKGROUND_REFLECTION_ON)
            GLES20.glFrontFace(GLES20.GL_CW)  // Front camera
        else
            GLES20.glFrontFace(GLES20.GL_CCW)   // Back camera

        // We get a list of views which depend on the mode we are working on, for mono we have
        // only one view, in stereo we have three: left, right and postprocess
        val viewList = mRenderingPrimitives!!.renderingViews

        // Cycle through the view list
        for (v in 0 until viewList.numViews) {
            // Get the view id
            val viewID = viewList.getView(v.toInt())

            val viewport: Vec4I
            // Get the viewport for that specific view
            viewport = mRenderingPrimitives!!.getViewport(viewID)

            // Set viewport for current view
            GLES20.glViewport(viewport.data[0], viewport.data[1], viewport.data[2], viewport.data[3])

            // Set scissor
            GLES20.glScissor(viewport.data[0], viewport.data[1], viewport.data[2], viewport.data[3])

            // Get projection matrix for the current view. COORDINATE_SYSTEM_CAMERA used for AR and
            // COORDINATE_SYSTEM_WORLD for VR
            val projMatrix = mRenderingPrimitives!!.getProjectionMatrix(viewID, COORDINATE_SYSTEM_TYPE.COORDINATE_SYSTEM_CAMERA)

            // Create GL matrix setting up the near and far planes
            val rawProjectionMatrixGL = Tool.convertPerspectiveProjection2GLMatrix(projMatrix, mNearPlane, mFarPlane).data

            // Apply the appropriate eye adjustment to the raw projection matrix, and assign to the global variable
            val eyeAdjustmentGL = Tool.convert2GLMatrix(mRenderingPrimitives!!.getEyeDisplayAdjustmentMatrix(viewID)).data

            val projectionMatrix = FloatArray(16)
            // Apply the adjustment to the projection matrix
            Matrix.multiplyMM(projectionMatrix, 0, rawProjectionMatrixGL, 0, eyeAdjustmentGL, 0)

            currentView = viewID

            // Call renderFrame from the app renderer class which implements SampleAppRendererControl
            // This will be called for MONO, LEFT and RIGHT views, POSTPROCESS will not render the
            // frame
            if (currentView != VIEW.VIEW_POSTPROCESS) renderFrame(state, projectionMatrix)
        }

        renderer.end()
    }

    fun renderFrame(state: State, projectionMatrix: FloatArray) {
        // did we find any trackables this frame?
        for (index in 0 until state.numTrackableResults) {
            //remember trackable
            val trackableResult: TrackableResult = state.getTrackableResult(index)

            val modelViewMatrix_Vuforia: Matrix44F = Tool.convertPose2GLMatrix(trackableResult.pose)
            val modelViewMatrix: FloatArray = modelViewMatrix_Vuforia.data

            val rotated: FloatArray
            if (renderer.videoBackgroundConfig.reflection == VIDEO_BACKGROUND_REFLECTION.VIDEO_BACKGROUND_REFLECTION_ON) {
                // Front camera
                rotated = floatArrayOf(
                    modelViewMatrix[1], modelViewMatrix[0], modelViewMatrix[2], modelViewMatrix[3],
                    modelViewMatrix[5], modelViewMatrix[4], modelViewMatrix[6], modelViewMatrix[7],
                    modelViewMatrix[9], modelViewMatrix[8], modelViewMatrix[10], modelViewMatrix[11],
                    modelViewMatrix[13], modelViewMatrix[12], modelViewMatrix[14], modelViewMatrix[15]
                )
            } else {
                // Back camera
                rotated = floatArrayOf(
                    modelViewMatrix[1], -modelViewMatrix[0], modelViewMatrix[2], modelViewMatrix[3],
                    modelViewMatrix[5], -modelViewMatrix[4], modelViewMatrix[6], modelViewMatrix[7],
                    modelViewMatrix[9], -modelViewMatrix[8], modelViewMatrix[10], modelViewMatrix[11],
                    modelViewMatrix[13], -modelViewMatrix[12], modelViewMatrix[14], modelViewMatrix[15]
                )
            }
            val rot = Matrix44F()
            rot.data = rotated
            val inverse = SampleMath.Matrix44FInverse(rot)
            val transp = SampleMath.Matrix44FTranspose(inverse)

            val modelViewProjection = FloatArray(16)
            Matrix.translateM(modelViewMatrix, 0, -20f, -250f, 2000f)
            Matrix.rotateM(modelViewMatrix, 0, 160f, 1f, 0f, 0f)
            Matrix.scaleM(modelViewMatrix, 0, 0.00001f, 0.00001f, 0.00001f)
            Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelViewMatrix, 0)

            arDetectListeners.forEach { it.onARDetected(trackableResult.trackable.id, transp.data, modelViewProjection) }
        }
    }

    /**
     * Configures the video mode and sets offsets for the camera's image
     */
    private fun configureVideoBackground() {
        if (screenWidth == null || screenHeight == null || isPortrait == null) {
            throw RuntimeException("Renderer has not been initialized yet")
        }

        val cameraDevice = CameraDevice.getInstance()
        val vm = cameraDevice.getVideoMode(CameraDevice.MODE.MODE_DEFAULT)

        val config = VideoBackgroundConfig()
        config.enabled = true
        config.position = Vec2I(0, 0)

        var xSize: Int
        var ySize: Int

        // We keep the aspect ratio to keep the video correctly rendered. If it is portrait we
        // preserve the height and scale width and vice versa if it is landscape, we preserve
        // the width and we check if the selected values fill the screen, otherwise we invert
        // the selection
        if (isPortrait!!) {
            xSize = (vm.height * (screenHeight!! / vm.width.toFloat())).toInt()
            ySize = screenHeight!!

            if (xSize < screenWidth!!) {
                xSize = screenWidth!!
                ySize = (screenWidth!! * (vm.width / vm.height.toFloat())).toInt()
            }
        } else {
            xSize = screenWidth!!
            ySize = (vm.height * (screenWidth!! / vm.width.toFloat())).toInt()

            if (ySize < screenHeight!!) {
                xSize = (screenHeight!! * (vm.width / vm.height.toFloat())).toInt()
                ySize = screenHeight!!
            }
        }

        config.size = Vec2I(xSize, ySize)

        Log.i("", "Configure Video Background : Video (" + vm.width
                + " , " + vm.height + "), Screen (" + screenWidth + " , "
                + screenHeight + "), mSize (" + xSize + " , " + ySize + ")")

        Renderer.getInstance().videoBackgroundConfig = config
    }

    /**
     * Render the video background, which is the camera
     */
    private fun renderVideoBackground() {
        if (currentView == VIEW.VIEW_POSTPROCESS)
            return

        mRenderingPrimitives = Device.getInstance().renderingPrimitives

        val vbVideoTextureUnit = 0
        // Bind the video bg texture and get the Texture ID from Vuforia
        videoBackgroundTex?.textureUnit = vbVideoTextureUnit
        if (!renderer.updateVideoBackgroundTexture(videoBackgroundTex)) {
            Log.e("", "Unable to update video background texture")
            return
        }

        val vbProjectionMatrix: FloatArray = Tool.convert2GLMatrix(mRenderingPrimitives!!.getVideoBackgroundProjectionMatrix(currentView, COORDINATE_SYSTEM_TYPE.COORDINATE_SYSTEM_CAMERA)).data

        // Apply the scene scale on video see-through eyewear, to scale the video background and augmentation
        // so that the display lines up with the real world
        // This should not be applied on optical see-through devices, as there is no video background,
        // and the calibration ensures that the augmentation matches the real world
        if (Device.getInstance().isViewerActive) {
            val sceneScaleFactor: Float = getSceneScaleFactor().toFloat()
            Matrix.scaleM(vbProjectionMatrix, 0, sceneScaleFactor, sceneScaleFactor, 1.0f)
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST)

        val vbMesh = mRenderingPrimitives!!.getVideoBackgroundMesh(currentView)
        // Load the shader and upload the vertex/texcoord/index data
        GLES20.glUseProgram(vbShaderProgramID)
        GLES20.glVertexAttribPointer(vbVertexHandle, 3, GLES20.GL_FLOAT, false, 0, vbMesh.positions)
        GLES20.glVertexAttribPointer(vbTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, vbMesh.uVs)

        GLES20.glUniform1i(vbTexSampler2DHandle, vbVideoTextureUnit)

        // Render the video background with the custom shader
        // First, we enable the vertex arrays
        GLES20.glEnableVertexAttribArray(vbVertexHandle)
        GLES20.glEnableVertexAttribArray(vbTexCoordHandle)

        // Pass the projection matrix to OpenGL
        GLES20.glUniformMatrix4fv(vbProjectionMatrixHandle, 1, false, vbProjectionMatrix, 0)

        // Then, we issue the render call
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, vbMesh.numTriangles * 3, GLES20.GL_UNSIGNED_SHORT, vbMesh.triangles)

        // Finally, we disable the vertex arrays
        GLES20.glDisableVertexAttribArray(vbVertexHandle)
        GLES20.glDisableVertexAttribArray(vbTexCoordHandle)
    }

    private fun getSceneScaleFactor(): Double {
        val virtualForYDegs = 85.0f
        val pi = 3.14159f

        // Get the y-dimension of the physical camera field of view
        val fovVector = CameraDevice.getInstance().cameraCalibration.fieldOfViewRads
        val cameraFovYRads = fovVector.data[1]

        // Get the y-dimension of the virtual camera field of view
        val virtualFovYRads = virtualForYDegs * pi / 180

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

    /**
     * Remove AR with specified id
     * @param id of AR point
     */
    override fun removeAR(id: Int) {
        arAppSession.destroyTrackableSource(id)
    }
}