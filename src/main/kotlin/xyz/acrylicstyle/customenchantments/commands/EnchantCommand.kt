package xyz.acrylicstyle.customenchantments.commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import xyz.acrylicstyle.customenchantments.CustomEnchantmentsPlugin

object EnchantCommand : CECommand<Player>(Player::class) {
    private val enchantOption = mutableListOf("--force", "--anti")
    override val name = "enchant"

    override fun execute(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage(Component.text("/ce enchant <ID> <Level> [--force] [--anti]", NamedTextColor.RED))
            return
        }
        val argList = args.toMutableList()
        val force = argList.contains("--force")
        if (force) argList.remove("--force")
        val anti = argList.contains("--anti")
        if (anti) argList.remove("--anti")
        val item = player.inventory.itemInMainHand
        val enchantment = CustomEnchantmentsPlugin.instance.getManager().getById(argList[0])
        if (enchantment == null) {
            player.sendMessage(Component.text("Cannot find enchantment by " + argList[0], NamedTextColor.RED))
            return
        }
        if (CustomEnchantmentsPlugin.instance.getManager().hasEnchantment(item, enchantment)) {
            player.sendMessage(Component.text("Enchantment " + enchantment.name + " is already enchanted on this item", NamedTextColor.RED))
            return
        }
        val level = if (argList[1] == "max") {
            enchantment.maxLevel
        } else {
            argList[1].toIntOrNull() ?: run {
                player.sendMessage(Component.text("Value isn't valid number: " + args[1], NamedTextColor.RED))
                return
            }
        }
        if (!force && enchantment.maxLevel < level) {
            player.sendMessage(Component.text("Cannot apply the enchantment with level higher than maximum level specified by enchantment. Use --force to apply enchantment forcefully.", NamedTextColor.RED))
            return
        }
        if (!force && enchantment.startLevel > level) {
            player.sendMessage(Component.text("Cannot apply the enchantment with level lower than minimum level specified by enchantment. Use --force to apply enchantment forcefully.", NamedTextColor.RED))
            return
        }
        if (item.type.isAir) {
            player.sendMessage(Component.text("Cannot apply the enchantment to the air", NamedTextColor.RED))
            return
        }
        if (!force && !enchantment.canEnchantItem(item)) {
            player.sendMessage(Component.text("Cannot apply the enchantment to this item. Use --force to bypass check.", NamedTextColor.RED))
            return
        }
        player.inventory.setItemInMainHand(CustomEnchantmentsPlugin.instance.getManager().applyEnchantment(item, enchantment, level, anti))
        player.sendMessage(Component.text("Applied the enchantment successfully.", NamedTextColor.GREEN))
    }

    override fun suggest(player: Player, args: Array<out String>): List<String> {
        if (args.size == 1) {
            return CustomEnchantmentsPlugin
                .instance
                .getManager()
                .getEnchantments()
                .map { enchantment -> enchantment.key.toString() }
                .filter { it.startsWith(args[0]) || it.replace(".*:(.*)".toRegex(), "$1").startsWith(args[0]) }
        }
        if (args.size >= 2) {
            return enchantOption.toMutableList().apply { removeAll(args.toSet()) }.filter { it.startsWith(args.last()) }
        }
        return emptyList()
    }
}
