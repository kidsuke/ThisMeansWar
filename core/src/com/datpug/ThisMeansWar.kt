package com.datpug

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.datpug.ar.ARRenderer
import com.datpug.controller.ChallengeController
import com.datpug.controller.MonsterController
import com.datpug.controller.PlayerController
import com.datpug.controller.RemoteController
import com.datpug.util.GameAssets

class ThisMeansWar(val arRenderer: ARRenderer): ApplicationAdapter() {

    var isGameReady = false

    private var arDetectListener: ARRenderer.OnARDetectListener = object : ARRenderer.OnARDetectListener {
        override fun onARDetected(id: Int, cameraProjection: FloatArray, modelViewProjection: FloatArray) {
            if (isGameReady) {
                MonsterController.generateMonster(modelViewProjection)
                MonsterController.setCameraProjection(cameraProjection)
            }
        }
    }

    override fun create() {
        // Initialize AR Renderer
        arRenderer.initRendering(Gdx.graphics.width, Gdx.graphics.height)
        arRenderer.addOnARRenderListener(arDetectListener)

        // Initialize controllers
        PlayerController.create()
        MonsterController.create()
        ChallengeController.create()

        // Load assets
        GameAssets.loadAssets()
    }

    override fun pause() {
        super.pause()
        PlayerController.pause()
    }

    override fun resume() {
        super.resume()
        PlayerController.resume()
    }

    override fun render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        arRenderer.render()
        if (isGameReady) {
            GameManager.update()
            MonsterController.render()
            PlayerController.render()
            ChallengeController.render()
        }
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        arRenderer.resize(width, height)
    }

    override fun dispose() {
        PlayerController.dispose()
        MonsterController.dispose()
        ChallengeController.dispose()
        GameAssets.dispose()
        arRenderer.removeOnARRenderListener(arDetectListener)
    }

    fun setRemoteController(remoteController: RemoteController?) {
        PlayerController.setRemoteController(remoteController)
    }
}
