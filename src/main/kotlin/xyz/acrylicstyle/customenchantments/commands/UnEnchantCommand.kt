package xyz.acrylicstyle.customenchantments.commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import xyz.acrylicstyle.customenchantments.CustomEnchantmentsPlugin

object UnEnchantCommand : CECommand<Player>(Player::class) {
    override val name = "unenchant"

    override fun execute(player: Player, args: Array<out String>) {
        if (args.isEmpty()) {
            player.sendMessage(Component.text("/ce unenchant <ID>", NamedTextColor.RED))
            return
        }
        val enchantment = CustomEnchantmentsPlugin.instance.getManager().getById(args[0])
        if (enchantment == null) {
            player.sendMessage(Component.text("Cannot find enchantment by " + args[0], NamedTextColor.RED))
            return
        }
        val item = player.inventory.itemInMainHand
        if (!CustomEnchantmentsPlugin.instance.getManager().hasEnchantment(item, enchantment)) {
            player.sendMessage(Component.text("Enchantment " + enchantment.name + " is not enchanted on this item", NamedTextColor.RED))
            return
        }
        if (item.type.isAir) {
            player.sendMessage(Component.text("Cannot remove the enchantment from the air", NamedTextColor.RED))
            return
        }
        player.inventory.setItemInMainHand(CustomEnchantmentsPlugin.instance.getManager().removeEnchantment(item, enchantment))
        player.sendMessage(Component.text("Removed the enchantment successfully.", NamedTextColor.GREEN))
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
        return emptyList()
    }
}
