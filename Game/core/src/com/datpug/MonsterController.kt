package com.datpug

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btBoxShape
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.datpug.entity.Monster

/**
 * Created by longv on 30-Sep-17.
 */
class MonsterController: ApplicationListener {

    private lateinit var modelBatch: ModelBatch
    private lateinit var camera: PerspectiveCamera
    private lateinit var environment: Environment

    var monsters: List<Monster> = listOf()
        private set

    override fun create() {
        // Initialize batches
        modelBatch = ModelBatch()

        // Initialize camera
        camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.apply {
            position.set(10f, 10f, 10f)
            lookAt(0f, 0f, 0f)
            near = 10f
            far = 5000f
            update()
        }

        // Initialize environment
        environment = Environment()
        environment.apply {
            set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
            add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))
        }
    }

    override fun render() {
        modelBatch.begin(camera)
        modelBatch.render(monsters)
        modelBatch.end()
    }

    override fun pause() {}

    override fun resume() {}

    override fun resize(width: Int, height: Int) {}

    override fun dispose() {
        // Dispose all monsters if there is still any left
        monsters.forEach {
            // Remove the game object from the collision world
            CollisionWorld.instance.removeCollisionObject(it.body)
            // Dispose the game object
            it.dispose()
        }
        monsters = listOf()
    }

    fun generateMonster() {
        // Setup a new monster
        val newMonster = Monster(GameAssets.archerModel)
        newMonster.transform.scale(0.01f, 0.01f, 0.01f)
        newMonster.body.collisionShape = btBoxShape(Vector3(10f, 10f, 10f))
        newMonster.body.worldTransform = newMonster.transform
        // Add to list of monsters
        monsters = monsters.plus(newMonster)
        // Add to collision world to get notified when some object collide with it
        CollisionWorld.instance.addCollisionObject(newMonster.body, CollisionWorld.MONSTER_FLAG, CollisionWorld.BULLET_FLAG)
    }
}