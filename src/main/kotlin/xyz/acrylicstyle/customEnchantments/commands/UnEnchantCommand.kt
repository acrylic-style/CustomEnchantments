package xyz.acrylicstyle.customEnchantments.commands

import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import xyz.acrylicstyle.customEnchantments.CustomEnchantmentsPlugin
import xyz.acrylicstyle.tomeito_api.subcommand.PlayerSubCommandExecutor
import xyz.acrylicstyle.tomeito_api.subcommand.SubCommand

@SubCommand(name = "unenchant", usage = "/ce unenchant <ID>", description = "Removes the enchantment.")
class UnEnchantCommand : PlayerSubCommandExecutor() {
    override fun onCommand(player: Player, args: Array<String>) {
        if (args.isEmpty()) {
            player.sendMessage(ChatColor.RED.toString() + "/ce unenchant <ID>")
            return
        }
        val enchantment = CustomEnchantmentsPlugin.instance.getManager().getById(args[0])
        if (enchantment == null) {
            player.sendMessage(ChatColor.RED.toString() + "Cannot find enchantment by " + args[0])
            return
        }
        val item = player.inventory.itemInHand
        if (!CustomEnchantmentsPlugin.instance.getManager().hasEnchantment(item, enchantment)) {
            player.sendMessage(ChatColor.RED.toString() + "Enchantment " + enchantment.name + " is not enchanted on this item")
            return
        }
        if (item.type == Material.AIR) {
            player.sendMessage(ChatColor.RED.toString() + "Cannot remove the enchantment from the air")
            return
        }
        player.inventory.itemInHand = CustomEnchantmentsPlugin.instance.getManager().removeEnchantment(item, enchantment)
        player.sendMessage(ChatColor.GREEN.toString() + "Removed the enchantment successfully.")
    }
}
