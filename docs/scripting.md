# SuraScript reference

SuraScript is the embedded scripting language of Sura Engine. It is designed for gameplay logic:
fast to start, no compilation step on device, safe to run every frame.

## Language

```js
// variables
var speed = 120
var name = "player"
var alive = true

// functions
function damage(amount) {
    health = health - amount
    if (health <= 0) {
        alive = false
    }
    return health
}

// loops
for i in 0..10 { move(1, 0) }
while (alive) { tick() }
```

Supported syntax: `var/let`, `if/else`, `while`, `for <var> in <from>..<to>`, `break`, `continue`,
`function`, `return`, `and`/`or`/`not` (plus `&&`, `||`, `!`), ternary `cond ? a : b`, string
literals with escapes, comments (`//` and `/* */`), member access (`text.length`), indexing and
assignment operators (`=`, `+=`, `-=`, `*=`, `/=`, `%=`).

Values: numbers (double), strings, booleans, `null`, lists and maps (created by the engine and
passable to scripts).

## Lifecycle hooks

```js
function start()              { }   // once, when the component starts
function update(dt)           { }   // every frame, dt in seconds
function fixedUpdate(dt)      { }   // fixed timestep (physics)
function onCollision(other)   { }   // non-trigger collision with another object (name)
function onTriggerEnter(o)    { }   // trigger overlap entered
function onDestroy()          { }   // component removed
```

## Engine API

### Transform
`x`, `y` (read/write), `rotation`, `scaleX`, `scaleY`, `active`,
`move(dx, dy)`, `setPosition(x, y)`.

### Physics
`setVelocity(x, y)`, `velocityX()`, `velocityY()`, `addForce(x, y)`, `isGrounded(groundY)`.

### Scene
`spawn(name, x, y)`, `destroySelf()`, `find(name)`, `objectX(name)`, `objectY(name)`,
`distanceTo(name)`, `objectCount()`.

### Input
`touchActive()`, `touchX()`, `touchY()`, `screenWidth()`, `screenHeight()`.

### Time and math
`engineTime()`, `deltaTime()`, `random(min, max)`, `abs`, `floor`, `ceil`, `round`, `sqrt`,
`sin`, `cos`, `min`, `max`, `clamp(v, min, max)`, `lerp(a, b, t)`.

### Strings and collections
`+` concatenation, `.length`, `.upper()`, `.lower()`, `.trim()`, `.split(sep)`, `.contains(s)`,
`.substring(start, end)`, `.toString()`, `len(value)`, `keys(map)`, `type(value)`.

### Debug
`print(...)`, `log("message")` — both appear in the editor console and logcat.

## Custom natives

From Kotlin you can extend the API of a script:

```kotlin
val behaviour = ScriptBehaviour().apply {
    scriptName = "Enemy"
    source = "function update(dt) { if (playerNear()) { charge() } }"
    compile()
}
behaviour.registerFunction("playerNear") { _ -> distanceToPlayer() < 120f }
behaviour.registerFunction("charge") { _ -> startCharge(); null }
```

## Error handling

* Syntax errors are reported with line numbers before the script runs.
* Runtime errors (unknown function, bad index) stop the current call and are logged;
  the rest of the frame keeps running.
* Infinite loops are terminated by the step budget and reported as
  `script exceeded step budget` in the console.
