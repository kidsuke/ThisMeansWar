package com.datpug

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.AnimationController
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btBoxShape
import com.datpug.entity.Monster

/**
 * Created by longv on 30-Sep-17.
 */
class MonsterController: ApplicationListener {

    private lateinit var modelBatch: ModelBatch
    private lateinit var camera: PerspectiveCamera
    private lateinit var environment: Environment

    private var animationControllers: List<AnimationController> = listOf()

    var monsterDeadListener: OnMonsterDeadListener? = null
    var monsters: Map<Int, Monster> = mapOf()
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
        removeDeadMonsters()
        updateAnimationControllers()

        modelBatch.begin(camera)
        modelBatch.render(monsters.values)
        modelBatch.end()
    }

    override fun pause() {}

    override fun resume() {}

    override fun resize(width: Int, height: Int) {}

    override fun dispose() {
        // Dispose all monsters if there is still any left
        monsters.forEach {
            // Remove the game object from the collision world
            CollisionWorld.instance.removeCollisionObject(it.value.body)
            // Dispose the game object
            it.value.dispose()
        }
        monsters = mapOf()
    }

    private fun updateAnimationControllers() {
        animationControllers.forEach { it.update(Gdx.graphics.deltaTime) }
    }

    private fun removeDeadMonsters() {
        monsters.forEach {
            if (it.value.isDead) {
                CollisionWorld.instance.removeCollisionObject(it.value.body)
                it.value.dispose()
                monsterDeadListener?.onMonsterDead(Pair(it.key, it.value))
            }
        }
        monsters = monsters.filterNot { it.value.isDisposed }
    }

    fun generateMonster(id: Int) {
        if (!monsters.keys.contains(id)) {
            // Setup a new monster
            val newMonster = Monster(GameAssets.cerberusModel)
            newMonster.transform.scale(0.01f, 0.01f, 0.01f)
            newMonster.transform.translate(0f, 0f, -750f)
            newMonster.body.collisionShape = btBoxShape(Vector3(10f, 10f, 10f))
            newMonster.body.worldTransform = newMonster.transform
            newMonster.body.userIndex = monsters.size
            newMonster.body.contactCallbackFlag = CollisionWorld.MONSTER_FLAG
            // Add to list of monsters
            monsters = monsters.plus(Pair(id, newMonster))
            // Create animation controller for this monster
            val controller = AnimationController(newMonster)
            controller.setAnimation("Armature|Walk", -1)
            animationControllers = animationControllers.plus(controller)
            // Add to collision world to get notified when some object collide with it
            CollisionWorld.instance.addCollisionObject(newMonster.body)
        }
    }

    fun setCameraProjection(data: FloatArray, fieldOfView: Float) {
        camera.position.set(data[12], data[13], data[14])
        camera.up.set(data[4], data[5], data[6])
        camera.direction.set(data[8], data[9], data[10])
        camera.fieldOfView = fieldOfView
        camera.update()
    }

    interface OnMonsterDeadListener {
        fun onMonsterDead(monster: Pair<Int, Monster>)
    }
}