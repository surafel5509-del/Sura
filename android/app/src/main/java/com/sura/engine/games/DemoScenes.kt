package com.sura.engine.games

import com.sura.engine.animation.Animator
import com.sura.engine.particles.ParticleSystem
import com.sura.engine.physics.BodyType
import com.sura.engine.physics.BoxCollider2D
import com.sura.engine.physics.RigidBody2D
import com.sura.engine.rendering.CameraComponent
import com.sura.engine.rendering.Light2D
import com.sura.engine.rendering.SpriteRenderer
import com.sura.engine.scene.GameObject
import com.sura.engine.scene.Scene
import com.sura.engine.scripting.ScriptBehaviour
import com.sura.engine.tilemap.Tilemap
import com.sura.engine.tilemap.TilemapComponent
import com.sura.engine.utils.Color
import kotlin.random.Random

/**
 * Ready to play demo games that prove the engine end to end:
 * tilemaps, physics, scripts, particles, lights and cameras.
 */
object DemoScenes {

    fun names(): List<String> = listOf("platformer", "shooter", "pong", "topdown", "particles", "empty")

    fun build(name: String): Scene = when (name.lowercase()) {
        "platformer" -> platformer()
        "shooter" -> shooter()
        "pong" -> pong()
        "topdown" -> topdown()
        "particles" -> particleShowcase()
        else -> empty()
    }

    fun empty(): Scene {
        val scene = Scene("MainScene")
        createCamera(scene, Color(0.13f, 0.14f, 0.20f, 1f))
        return scene
    }

    // ------------------------------------------------------------------ platformer

    fun platformer(): Scene {
        val scene = Scene("PlatformerDemo")
        createCamera(scene, Color(0.10f, 0.12f, 0.22f, 1f))

        val level = scene.createGameObject("Level")
        val tilemap = Tilemap(40, 20, 32f)
        val ground = tilemap.addLayer("Ground")
        ground.solid = true
        ground.color = Color(0.28f, 0.40f, 0.32f, 1f)
        for (x in 0 until 40) {
            ground.setTile(x, 13, 1)
            ground.setTile(x, 14, 1)
            ground.setTile(x, 15, 1)
        }
        for (x in 6..10) ground.setTile(x, 9, 1)
        for (x in 14..18) ground.setTile(x, 7, 1)
        for (x in 22..24) ground.setTile(x, 10, 1)
        val tilemapComponent = TilemapComponent()
        tilemapComponent.tilemap = tilemap
        tilemapComponent.tileSize = 32f
        level.addComponent(tilemapComponent)

        val player = scene.createGameObject("Player")
        player.transform.position.set(96f, 380f)
        player.tag = "Player"
        val playerRenderer = SpriteRenderer()
        playerRenderer.size.set(28f, 40f)
        playerRenderer.color = Color(0.54f, 0.71f, 0.98f, 1f)
        player.addComponent(playerRenderer)
        val body = RigidBody2D()
        body.bodyType = BodyType.KINEMATIC
        body.useGravity = false
        player.addComponent(body)
        val collider = BoxCollider2D()
        collider.size.set(28f, 40f)
        player.addComponent(collider)
        val playerScript = ScriptBehaviour()
        playerScript.scriptName = "PlatformerPlayer"
        playerScript.source = PLATFORMER_SCRIPT
        player.addComponent(playerScript)

        val camera = scene.findByName("Main Camera")?.getComponent<CameraComponent>()
        camera?.followTargetName = "Player"
        camera?.zoom = 1f

        addCoin(scene, 320f, 300f)
        addCoin(scene, 480f, 250f)
        addCoin(scene, 640f, 330f)

        val enemy = scene.createGameObject("Enemy")
        enemy.transform.position.set(520f, 400f)
        val enemyRenderer = SpriteRenderer()
        enemyRenderer.size.set(30f, 30f)
        enemyRenderer.color = Color(0.95f, 0.45f, 0.45f, 1f)
        enemy.addComponent(enemyRenderer)

        val torch = scene.createGameObject("Torch")
        torch.transform.position.set(200f, 300f)
        val light = Light2D()
        light.radius = 220f
        light.flicker = true
        torch.addComponent(light)

        val dust = scene.createGameObject("Dust")
        dust.transform.position.set(320f, 420f)
        val dustSystem = ParticleSystem()
        dustSystem.emissionRate = 12f
        dustSystem.lifetime = 2.5f
        dustSystem.speed = 18f
        dustSystem.gravity.set(0f, 6f)
        dustSystem.colorStart = Color(1f, 1f, 0.9f, 0.5f)
        dustSystem.colorEnd = Color(1f, 1f, 0.9f, 0f)
        dust.addComponent(dustSystem)
        return scene
    }

    private fun addCoin(scene: Scene, x: Float, y: Float) {
        val coin = scene.createGameObject("Coin")
        coin.transform.position.set(x, y)
        coin.tag = "Coin"
        val renderer = SpriteRenderer()
        renderer.size.set(18f, 18f)
        renderer.color = Color(0.98f, 0.85f, 0.35f, 1f)
        coin.addComponent(renderer)
        val animator = Animator()
        val clip = com.sura.engine.animation.AnimationClip("spin")
        clip.duration = 1.2f
        clip.loop = true
        clip.addKey("y", 0f, y)
        clip.addKey("y", 0.6f, y + 8f)
        clip.addKey("y", 1.2f, y)
        animator.addClip(clip)
        animator.play("spin")
        coin.addComponent(animator)
    }

    // ------------------------------------------------------------------ shooter

    fun shooter(): Scene {
        val scene = Scene("SpaceShooterDemo")
        createCamera(scene, Color(0.03f, 0.04f, 0.09f, 1f))

        val player = scene.createGameObject("Ship")
        player.transform.position.set(480f, 120f)
        val renderer = SpriteRenderer()
        renderer.size.set(34f, 34f)
        renderer.color = Color(0.48f, 0.78f, 0.98f, 1f)
        player.addComponent(renderer)
        val script = ScriptBehaviour()
        script.scriptName = "ShooterPlayer"
        script.source = SHOOTER_SCRIPT
        player.addComponent(script)

        for (i in 0 until 6) {
            val enemy = scene.createGameObject("Enemy$i")
            enemy.transform.position.set(120f + i * 140f, 560f)
            val enemyRenderer = SpriteRenderer()
            enemyRenderer.size.set(28f, 28f)
            enemyRenderer.color = Color(0.93f, 0.42f, 0.52f, 1f)
            enemy.addComponent(enemyRenderer)
        }

        val stars = scene.createGameObject("Stars")
        val particles = ParticleSystem()
        particles.emissionRate = 40f
        particles.lifetime = 4f
        particles.speed = 30f
        particles.gravity.set(0f, -30f)
        particles.sizeStart = 3f
        particles.sizeEnd = 1f
        particles.colorStart = Color(1f, 1f, 1f, 0.8f)
        particles.colorEnd = Color(1f, 1f, 1f, 0f)
        stars.addComponent(particles)
        return scene
    }

    // ------------------------------------------------------------------ pong

    fun pong(): Scene {
        val scene = Scene("PongDemo")
        createCamera(scene, Color(0.08f, 0.09f, 0.13f, 1f))
        val left = scene.createGameObject("PaddleLeft")
        left.transform.position.set(60f, 320f)
        left.addComponent(SpriteRenderer().apply { size.set(18f, 90f); color = Color(0.6f, 0.9f, 0.7f, 1f) })
        val right = scene.createGameObject("PaddleRight")
        right.transform.position.set(900f, 320f)
        right.addComponent(SpriteRenderer().apply { size.set(18f, 90f); color = Color(0.95f, 0.6f, 0.6f, 1f) })
        val ball = scene.createGameObject("Ball")
        ball.transform.position.set(480f, 320f)
        ball.addComponent(SpriteRenderer().apply { size.set(20f, 20f); color = Color(1f, 0.95f, 0.7f, 1f) })
        val script = ScriptBehaviour()
        script.scriptName = "PongBall"
        script.source = PONG_SCRIPT
        ball.addComponent(script)
        return scene
    }

    // ------------------------------------------------------------------ topdown

    fun topdown(): Scene {
        val scene = Scene("TopDownDemo")
        createCamera(scene, Color(0.11f, 0.13f, 0.11f, 1f))
        val walls = scene.createGameObject("Walls")
        val tilemap = Tilemap(30, 20, 32f)
        val wallLayer = tilemap.addLayer("Walls")
        wallLayer.solid = true
        wallLayer.color = Color(0.35f, 0.34f, 0.40f, 1f)
        for (x in 0 until 30) { wallLayer.setTile(x, 0, 1); wallLayer.setTile(x, 19, 1) }
        for (y in 0 until 20) { wallLayer.setTile(0, y, 1); wallLayer.setTile(29, y, 1) }
        for (x in 8..12) wallLayer.setTile(x, 8, 1)
        for (y in 12..16) wallLayer.setTile(18, y, 1)
        walls.addComponent(TilemapComponent().apply { this.tilemap = tilemap; tileSize = 32f })

        val hero = scene.createGameObject("Hero")
        hero.transform.position.set(200f, 300f)
        hero.addComponent(SpriteRenderer().apply { size.set(26f, 26f); color = Color(0.6f, 0.85f, 1f, 1f) })
        val script = ScriptBehaviour()
        script.scriptName = "TopDownHero"
        script.source = TOPDOWN_SCRIPT
        hero.addComponent(script)

        for (i in 0 until 4) {
            val enemy = scene.createGameObject("Slime$i")
            enemy.transform.position.set(400f + i * 90f, 200f + i * 60f)
            enemy.addComponent(SpriteRenderer().apply { size.set(24f, 24f); color = Color(0.7f, 0.9f, 0.5f, 1f) })
        }
        return scene
    }

    // ------------------------------------------------------------------ particles

    fun particleShowcase(): Scene {
        val scene = Scene("ParticleShowcase")
        createCamera(scene, Color(0.05f, 0.05f, 0.09f, 1f))
        val colors = listOf(
            Color(0.95f, 0.5f, 0.4f, 1f), Color(0.5f, 0.8f, 0.95f, 1f), Color(0.8f, 0.6f, 0.95f, 1f),
            Color(0.6f, 0.95f, 0.6f, 1f), Color(0.98f, 0.85f, 0.4f, 1f)
        )
        for (i in 0 until 5) {
            val emitter = scene.createGameObject("Emitter$i")
            emitter.transform.position.set(160f + i * 170f, 300f)
            val system = ParticleSystem()
            system.maxParticles = 300
            system.emissionRate = 60f
            system.lifetime = 2f
            system.speed = 90f
            system.speedVariance = 60f
            system.sizeStart = 10f
            system.sizeEnd = 0f
            system.colorStart = colors[i]
            system.colorEnd = Color(colors[i].r, colors[i].g, colors[i].b, 0f)
            system.shape = ParticleSystem.EmitShape.CIRCLE
            system.emitRadius = 6f
            system.gravity.set(0f, -20f)
            emitter.addComponent(system)
            val light = Light2D()
            light.color = colors[i]
            light.radius = 160f
            light.intensity = 0.6f
            emitter.addComponent(light)
        }
        return scene
    }

    // ------------------------------------------------------------------ helpers

    private fun createCamera(scene: Scene, background: Color): GameObject {
        val cameraObject = scene.createGameObject("Main Camera")
        cameraObject.tag = "MainCamera"
        cameraObject.transform.position.set(480f, 320f)
        cameraObject.addComponent(CameraComponent().apply { zoom = 1f })
        val ambient = scene.createGameObject("Ambient")
        val light = Light2D()
        light.color = Color(0.8f, 0.85f, 1f, 1f)
        light.radius = 900f
        light.intensity = 0.35f
        ambient.addComponent(light)
        return cameraObject
    }

    fun randomColor(): Color = Color(Random.nextFloat(), Random.nextFloat(), Random.nextFloat(), 1f)

    // ------------------------------------------------------------------ scripts

    private val PLATFORMER_SCRIPT = """
        // Platformer player - touch anywhere to jump, tilt free horizontal auto-run.
        var speed = 150
        var jump = 320
        var velocityY = 0
        var grounded = true
        var groundY = 400
        var direction = 1

        function start() {
            log("platformer player ready")
            groundY = y
        }

        function update(dt) {
            // gravity
            velocityY = velocityY - 900 * dt
            var nextY = y + velocityY * dt
            if (nextY <= groundY - 8) {
                y = nextY
                grounded = false
            } else {
                y = groundY
                velocityY = 0
                grounded = true
            }

            // touch controls: left half moves left, right half moves right
            if (touchActive()) {
                var tx = touchX()
                var half = screenWidth() / 2
                if (tx < half) {
                    move(0 - speed * dt, 0)
                    direction = 0 - 1
                } else {
                    move(speed * dt, 0)
                    direction = 1
                }
                if (grounded) {
                    velocityY = jump
                    grounded = false
                }
            }
        }
    """.trimIndent()

    private val SHOOTER_SCRIPT = """
        // Space shooter: touch to move the ship, it fires automatically.
        var fireCooldown = 0
        var fireRate = 0.25
        var score = 0

        function start() {
            log("shooter ready")
        }

        function update(dt) {
            if (touchActive()) {
                var targetX = touchX()
                var targetY = touchY()
                var dx = targetX - x
                var dy = targetY - y
                move(dx * 4 * dt, dy * 4 * dt)
            }
            fireCooldown = fireCooldown - dt
            if (fireCooldown <= 0) {
                spawn("Bullet", x, y + 24)
                fireCooldown = fireRate
            }
            // bullets drift up
            var count = objectCount()
            if (count > 400) {
                log("too many objects")
            }
        }
    """.trimIndent()

    private val PONG_SCRIPT = """
        var velocityX = 220
        var velocityY = 160
        var leftScore = 0
        var rightScore = 0

        function update(dt) {
            x = x + velocityX * dt
            y = y + velocityY * dt
            if (y < 40) {
                y = 40
                velocityY = 0 - velocityY
            }
            if (y > 600) {
                y = 600
                velocityY = 0 - velocityY
            }
            if (x < 90) {
                x = 480
                y = 320
                velocityX = 220
                rightScore = rightScore + 1
                log("right scores " + rightScore)
            }
            if (x > 880) {
                x = 480
                y = 320
                velocityX = 0 - 220
                leftScore = leftScore + 1
                log("left scores " + leftScore)
            }
        }
    """.trimIndent()

    private val TOPDOWN_SCRIPT = """
        var speed = 120
        var dirX = 1
        var dirY = 1

        function update(dt) {
            move(dirX * speed * dt, dirY * speed * dt)
            if (x > 800) { dirX = 0 - 1 }
            if (x < 100) { dirX = 1 }
            if (y > 560) { dirY = 0 - 1 }
            if (y < 100) { dirY = 1 }
        }
    """.trimIndent()
}
