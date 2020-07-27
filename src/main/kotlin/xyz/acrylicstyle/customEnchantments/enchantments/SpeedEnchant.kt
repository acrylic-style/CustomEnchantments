package xyz.acrylicstyle.customEnchantments.enchantments

import org.bukkit.ChatColor
import org.bukkit.enchantments.Enchantment
import org.bukkit.enchantments.EnchantmentTarget
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.acrylicstyle.customEnchantments.api.enchantment.CustomEnchantment

class SpeedEnchant : CustomEnchantment("speed") {
    override fun getDescription(): List<String> = listOf(ChatColor.YELLOW.toString() + "Increases speed.")

    override fun canEnchantItem(item: ItemStack): Boolean {
        return item.type.name.endsWith("BOOTS")
    }

    override fun getItemTarget(): EnchantmentTarget = EnchantmentTarget.ARMOR_FEET

    override fun getName(): String = "迅速" // todo: localization?

    override fun onActivate(player: Player, level: Int) {
        player.walkSpeed = player.walkSpeed + level * 0.02F
    }

    override fun onDeactivate(player: Player, level: Int) {
        player.walkSpeed = (player.walkSpeed - level * 0.02F).coerceAtLeast(0.2F)
    }

    override fun getMaxLevel(): Int = 5

    override fun getStartLevel(): Int = 1

    override fun conflictsWith(other: Enchantment): Boolean = false
}