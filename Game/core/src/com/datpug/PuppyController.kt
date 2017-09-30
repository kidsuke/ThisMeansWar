package com.datpug

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.Ray
import com.badlogic.gdx.physics.bullet.collision.ContactListener
import com.badlogic.gdx.physics.bullet.collision.btBoxShape
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject
import com.badlogic.gdx.utils.TimeUtils
import com.datpug.entity.GameObject
import com.datpug.entity.Monster

/**
 * Created by longv on 27-Sep-17.
 */

class PuppyController: ApplicationListener {

    private lateinit var modelBatch: ModelBatch
    private lateinit var perspectiveCamera: PerspectiveCamera
    private lateinit var environment: Environment

    private lateinit var projectileModel: Model
    private var leftProjectiles: List<GameObject> = listOf()
    private var rightProjectiles: List<GameObject> = listOf()
    private val leftProjectileSpawnPosition: Vector3 = Vector3(-20f, -10f, 0f)
    private val rightProjectileSpawnPosition: Vector3 = Vector3(20f, -10f, 0f)
    private val projectileDamage: Float = 50f
    private val firingSpeed: Float = 1f
    private val fireRate: Float = 0.2f
    private var startTime = TimeUtils.millis()

    var isFiring: Boolean = false
        private set
    private var target: Monster? = null

    override fun create() {
        modelBatch = ModelBatch()

        perspectiveCamera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        perspectiveCamera.apply {
            position.set(0f, 0f, 20f)
            lookAt(0f, 0f, 0f)
            near = 10f
            far = 5000f
            update()
        }

        environment = Environment()
        environment.apply {
            set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
            add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))
        }

    }

    override fun resize(width: Int, height: Int) {}

    override fun render() {
        if (isFiring) {
            fire()
        }
        removeUnusedProjectiles()
    }

    override fun pause() {}

    override fun resume() {}

    override fun dispose() {
        modelBatch.dispose()
        projectileModel.dispose()
    }

    private fun generateLeftProjectile(): GameObject {
        val projectile = GameObject(GameAssets.bulletModel, leftProjectileSpawnPosition)
        projectile.body.collisionShape = btBoxShape(Vector3(2.5f, 0.5f, 2.5f))
        projectile.transform.rotate(Vector3(1f, 1f, -1f), 90f)
        projectile.body.worldTransform = projectile.transform
        projectile.body.userData = projectile
        projectile.body.collisionFlags = projectile.body.collisionFlags.or(btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK)
        projectile.body.contactCallbackFlag = CollisionWorld.BULLET_FLAG
        projectile.body.contactCallbackFilter = CollisionWorld.MONSTER_FLAG
        CollisionWorld.instance.addCollisionObject(projectile.body)
        return projectile
    }

    private fun generateRightProjectile(): GameObject {
        val projectile = GameObject(GameAssets.bulletModel, rightProjectileSpawnPosition)
        projectile.body.collisionShape = btBoxShape(Vector3(2.5f, 0.5f, 2.5f))
        projectile.transform.rotate(Vector3(-1f, 1f, -1f), -90f)
        projectile.body.worldTransform = projectile.transform
        projectile.body.userData = projectile
        projectile.body.collisionFlags = projectile.body.collisionFlags.or(btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK)
        projectile.body.contactCallbackFlag = CollisionWorld.BULLET_FLAG
        projectile.body.contactCallbackFilter = CollisionWorld.MONSTER_FLAG
        CollisionWorld.instance.addCollisionObject(projectile.body)
        return projectile
    }


    private fun fire() {
        val timePassed: Float = (TimeUtils.timeSinceMillis(startTime) + Gdx.graphics.deltaTime).div(1000)
        if (timePassed >= fireRate) {
            leftProjectiles = leftProjectiles.plus(generateLeftProjectile())
            rightProjectiles = rightProjectiles.plus(generateRightProjectile())
            // Reset start time
            startTime = TimeUtils.millis()
        }

        // Update projectiles position
        leftProjectiles.forEach {
            val translation = target!!.center.sub(leftProjectileSpawnPosition).nor().scl(firingSpeed)
            it.transform.trn(translation)
            it.body.worldTransform = it.transform
        }
        rightProjectiles.forEach {
            val translation = target!!.center.sub(rightProjectileSpawnPosition).nor().scl(firingSpeed)
            it.transform.trn(translation)
            it.body.worldTransform = it.transform
        }

        // Render the projectiles
        modelBatch.begin(perspectiveCamera)
        leftProjectiles.plus(rightProjectiles).forEach {
            modelBatch.render(it, environment)
        }
        modelBatch.end()
    }

    private fun targetGetsHit(projectile: GameObject): Boolean = projectile.transform.getTranslation(Vector3()).dst(target!!.center) <= 1f

    fun startFire(target: Monster) {
        isFiring = true
        this.target = target
    }

    fun stopFire() {
        isFiring = false
        target = null
        leftProjectiles = listOf()
        rightProjectiles = listOf()
    }

    fun removeUnusedProjectiles() {
//        leftProjectiles.forEach { if (it.isDisposed) CollisionWorld.instance.removeCollisionObject(it.body) }
//        rightProjectiles.forEach { if (it.isDisposed) CollisionWorld.instance.removeCollisionObject(it.body) }
//        leftProjectiles = leftProjectiles.filterNot { it.isDisposed }
//        rightProjectiles = rightProjectiles.filterNot { it.isDisposed }
    }

    fun collidedProjectile() {
        target?.takeDamage(projectileDamage)
        //CollisionWorld.instance.removeCollisionObject(projectile.body)
        //projectile.dispose()
    }

}
