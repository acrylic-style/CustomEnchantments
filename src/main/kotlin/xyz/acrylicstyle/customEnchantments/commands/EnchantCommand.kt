package xyz.acrylicstyle.customEnchantments.commands

import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import util.ArgumentParser
import util.ICollectionList
import xyz.acrylicstyle.customEnchantments.CustomEnchantmentsPlugin
import xyz.acrylicstyle.tomeito_api.subcommand.PlayerSubCommandExecutor
import xyz.acrylicstyle.tomeito_api.subcommand.SubCommand
import xyz.acrylicstyle.tomeito_api.utils.TypeUtil

@SubCommand(name = "enchant", usage = "/ce enchant <ID> <Level> [--force]", description = "Applies the enchantment.")
class EnchantCommand : PlayerSubCommandExecutor() {
    override fun onCommand(player: Player, args_: Array<String>) {
        if (args_.size < 2) {
            player.sendMessage(ChatColor.RED.toString() + "/ce enchant <ID> <Level> [--force]")
            return
        }
        val args = ArgumentParser(ICollectionList.asList(args_).join(" ")).getArguments()
        val force = args.contains("force")
        if (force) args.remove("force")
        val item = player.inventory.itemInHand
        val enchantment = CustomEnchantmentsPlugin.instance.getManager().getById(args[0])
        if (enchantment == null) {
            player.sendMessage(ChatColor.RED.toString() + "Cannot find enchantment by " + args[0])
            return
        }
        if (CustomEnchantmentsPlugin.instance.getManager().hasEnchantment(item, enchantment)) {
            player.sendMessage(ChatColor.RED.toString() + "Enchantment " + enchantment.name + " is already enchanted on this item")
            return
        }
        if (!TypeUtil.isInt(args[1])) {
            player.sendMessage(ChatColor.RED.toString() + "Value isn't valid number: " + args[1])
            return
        }
        val level = Integer.parseInt(args[1])
        if (!force && enchantment.maxLevel < level) {
            player.sendMessage(ChatColor.RED.toString() + "Cannot apply the enchantment with level higher than maximum level specified by enchantment. Use --force to apply enchantment forcefully.")
            return
        }
        if (!force && enchantment.startLevel > level) {
            player.sendMessage(ChatColor.RED.toString() + "Cannot apply the enchantment with level lower than minimum level specified by enchantment. Use --force to apply enchantment forcefully.")
            return
        }
        if (item.type == Material.AIR) {
            player.sendMessage(ChatColor.RED.toString() + "Cannot apply the enchantment to the air")
            return
        }
        if (!force && !enchantment.canEnchantItem(item)) {
            player.sendMessage(ChatColor.RED.toString() + "Cannot apply the enchantment to this item. Use --force to bypass check.")
            return
        }
        player.inventory.itemInHand = CustomEnchantmentsPlugin.instance.getManager().applyEnchantment(item, enchantment, level)
        player.sendMessage(ChatColor.GREEN.toString() + "Applied the enchantment successfully.")
    }
}
