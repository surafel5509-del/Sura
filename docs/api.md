# Engine API cheat sheet

## Creating a scene

```kotlin
val scene = Scene("Level1")
val go = scene.createGameObject("Enemy")            // or createGameObject("Child", parent)
go.tag = "Enemy"
go.transform.position.set(240f, 120f)
go.transform.zOrder = 3
```

## Components

```kotlin
go.addComponent(SpriteRenderer().apply { size.set(32f, 32f) })
go.addComponent(RigidBody2D().apply { bodyType = BodyType.DYNAMIC })
go.addComponent(BoxCollider2D().apply { size.set(32f, 32f); restitution = 0.4f })
go.addComponent(Light2D().apply { radius = 200f; flicker = true })
go.addComponent(ParticleSystem().apply { emissionRate = 40f })
val sprite = go.getComponent<SpriteRenderer>()
go.removeComponent(sprite!!)
```

## Rendering

```kotlin
val renderer = GLRenderer()
renderer.initialize()
renderer.begin(camera)
SceneRenderer(renderer).render(scene, camera)
renderer.end()
```

## Physics queries

```kotlin
val world = Engine.get().services.get(PhysicsWorld::class.java)!!
val hit = world.raycast(Vector2(0f, 100f), Vector2(1f, 0f), 600f)
if (hit != null) { spawnImpact(hit.point, hit.normal) }
val overlapping = world.queryPoint(Vector2(100f, 100f))
```

## Scripting

```kotlin
val behaviour = ScriptBehaviour().apply {
    scriptName = "Spinner"
    source = """
        var angle = 0
        function update(dt) {
            angle = angle + 90 * dt
            rotation = angle
        }
    """.trimIndent()
    compile()
}
go.addComponent(behaviour)
```

## Serialization

```kotlin
val serializer = SceneSerializer()
val json = serializer.serialize(scene)              // -> String
project.writeText("Scenes/${scene.name}.scene", json)
val loaded = serializer.deserialize(json, "Level1")
```

## UI

```kotlin
val holder = go.addComponent(UIElementHolder())
holder.panel(20f, 20f, 300f, 120f)
holder.label("Score: 0", 40f, 40f, 24f)
holder.button("Restart", 40f, 80f, 160f, 44f) { restartGame() }
```

## Services

```kotlin
Engine.get().services.register(MyService::class.java, MyService())
val audio = Engine.get().services.get(AudioManager::class.java)
val jobs = Engine.get().services.get(JobSystem::class.java)
jobs?.run({ heavyLoad() }, { onLoaded() })
```
