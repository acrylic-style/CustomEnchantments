package xyz.acrylicstyle.customenchantments.commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import kotlin.reflect.KClass

abstract class CECommand<T : Any>(private val clazz: Class<T>) {
    constructor(clazz: KClass<T>) : this(clazz.java)

    abstract val name: String

    @Suppress("UNCHECKED_CAST")
    internal fun execute0(sender: CommandSender, args: Array<out String>) {
        if (!clazz.isInstance(sender)) {
            sender.sendMessage(Component.text("This command can only be used by ${clazz::class.java.simpleName}", NamedTextColor.RED))
            return
        }
        execute(sender as T, args)
    }

    @Suppress("UNCHECKED_CAST")
    internal fun suggest0(sender: CommandSender, args: Array<out String>): List<String>? {
        if (!clazz.isInstance(sender)) return emptyList()
        return suggest(sender as T, args)
    }

    abstract fun execute(player: T, args: Array<out String>)

    abstract fun suggest(player: T, args: Array<out String>): List<String>?
}
