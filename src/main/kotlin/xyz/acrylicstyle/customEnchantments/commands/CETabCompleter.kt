package xyz.acrylicstyle.customEnchantments.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import util.CollectionList
import xyz.acrylicstyle.customEnchantments.CustomEnchantmentsPlugin
import java.util.*

class CETabCompleter : TabCompleter {
    private fun filterArgsList(list: List<String>, s: String): List<String> = CollectionList(list).filter { s2: String ->
        s2.replace(".*:(.*)".toRegex(), "$1")
            .toLowerCase()
            .startsWith(s.replace(".*:(.*)".toRegex(), "$1").toLowerCase())
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        if (!sender.isOp) return Collections.emptyList()
        if (args.isEmpty()) return listOf("enchant", "unenchant")
        if (args.size == 1) return filterArgsList(listOf("enchant", "unenchant"), args[0])
        if (args.size == 2) {
            if (args[0] == "enchant" || args[0] == "unenchant") {
                return filterArgsList(
                    CustomEnchantmentsPlugin
                        .instance
                        .getManager()
                        .getEnchantments()
                        .map { enchantment -> enchantment.id },
                    args[1]
                )
            }
        }
        if (args.size == 4) {
            if (args[0] == "enchant") {
                return filterArgsList(listOf("--force"), args[3])
            }
        }
        return Collections.emptyList()
    }
}