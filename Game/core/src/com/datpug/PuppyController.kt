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
import com.badlogic.gdx.utils.TimeUtils
import com.datpug.entity.GameObject
import com.datpug.entity.Monster

/**
 * Created by longv on 27-Sep-17.
 */

class PuppyController(val camera: Camera) : ApplicationListener {

    private lateinit var modelBatch: ModelBatch
    private lateinit var assetManager: AssetManager
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

    private var loadingAssets = false
    var isFiring: Boolean = false
        private set
    private var target: Monster? = null

    override fun create() {
        modelBatch = ModelBatch()
        assetManager = AssetManager()

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

        loadAssets()
    }

    override fun resize(width: Int, height: Int) {}

    override fun render() {
        if (loadingAssets && assetManager.update()) {
            doneLoadingAssets()
        }

        if (!loadingAssets) {
            if (isFiring) {
                fire()
            }
        }
    }

    override fun pause() {}

    override fun resume() {}

    override fun dispose() {
        modelBatch.dispose()
        projectileModel.dispose()
    }

    private fun generateLeftProjectile(): GameObject {
        val projectile = GameObject(projectileModel, leftProjectileSpawnPosition)
        projectile.transform.rotate(Vector3(1f, 1f, -1f), 90f)
        return projectile
    }

    private fun generateRightProjectile(): GameObject {
        val projectile = GameObject(projectileModel, rightProjectileSpawnPosition)
        projectile.transform.rotate(Vector3(-1f, 1f, -1f), -90f)
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
        }
        rightProjectiles.forEach {
            val translation = target!!.center.sub(rightProjectileSpawnPosition).nor().scl(firingSpeed)
            it.transform.trn(translation)
        }

        // Check if target gets hit and remove projectile
        leftProjectiles = leftProjectiles.filterNot {
            val getsHit: Boolean = targetGetsHit(it)
            if (getsHit) target!!.takeDamage(projectileDamage)
            getsHit
        }
        rightProjectiles = rightProjectiles.filterNot {
            val getsHit: Boolean = targetGetsHit(it)
            if (getsHit) target!!.takeDamage(projectileDamage)
            getsHit
        }

        // Render the projectiles
        modelBatch.begin(perspectiveCamera)
        leftProjectiles.plus(rightProjectiles).forEach {
            modelBatch.render(it, environment)
        }
        modelBatch.end()
    }

    private fun targetGetsHit(projectile: GameObject): Boolean = projectile.transform.getTranslation(Vector3()).dst(target!!.center) <= 1f

    private fun loadAssets() {
        assetManager.load("mesh/bullet.g3db", Model::class.java)
        loadingAssets = true
    }

    private fun doneLoadingAssets() {
        projectileModel = assetManager.get("mesh/bullet.g3db", Model::class.java)
        loadingAssets = false
    }

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
}
