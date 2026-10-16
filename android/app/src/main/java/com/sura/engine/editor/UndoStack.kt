package com.sura.engine.editor

/** Command based undo / redo history used by every editor mutation. */
class UndoStack(private val limit: Int = 100) {
    interface Command {
        val label: String
        fun undo()
        fun redo()
    }

    class LambdaCommand(override val label: String, val onUndo: () -> Unit, val onRedo: () -> Unit) : Command {
        override fun undo() { onUndo() }
        override fun redo() { onRedo() }
    }

    private val undoCommands = ArrayDeque<Command>()
    private val redoCommands = ArrayDeque<Command>()

    val canUndo: Boolean get() = undoCommands.isNotEmpty()
    val canRedo: Boolean get() = redoCommands.isNotEmpty()
    val undoLabel: String get() = undoCommands.lastOrNull()?.label ?: ""
    val redoLabel: String get() = redoCommands.lastOrNull()?.label ?: ""
    val size: Int get() = undoCommands.size

    fun push(command: Command) {
        undoCommands.addLast(command)
        while (undoCommands.size > limit) undoCommands.removeFirst()
        redoCommands.clear()
    }

    fun push(label: String, onUndo: () -> Unit, onRedo: () -> Unit) {
        push(LambdaCommand(label, onUndo, onRedo))
    }

    fun undo(): Boolean {
        val command = undoCommands.removeLastOrNull() ?: return false
        command.undo()
        redoCommands.addLast(command)
        return true
    }

    fun redo(): Boolean {
        val command = redoCommands.removeLastOrNull() ?: return false
        command.redo()
        undoCommands.addLast(command)
        return true
    }

    fun clear() { undoCommands.clear(); redoCommands.clear() }
}
