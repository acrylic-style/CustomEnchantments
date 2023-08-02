package xyz.acrylicstyle.customenchantments.commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import java.util.*

object RootCommand : TabExecutor {
    private val commands = listOf(EnchantCommand, UnEnchantCommand)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(Component.text("/ce <arguments>", NamedTextColor.RED))
            return true
        }
        val cmd = commands.find { it.name == args[0] } ?: run {
            sender.sendMessage(Component.text("Unknown sub-command: ${args[0]}", NamedTextColor.RED))
            return true
        }
        cmd.execute0(sender, args.slice(1..<args.size).toTypedArray())
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String>? {
        if (args.size == 1) return commands.map { it.name }.filter { it.startsWith(args[0]) }
        if (args.size >= 2) {
            return (commands.find { it.name == args[0] } ?: return emptyList()).suggest0(sender, args.slice(1..<args.size).toTypedArray())
        }
        return Collections.emptyList()
    }
}
