package xyz.acrylicstyle.customenchantments.enchantments

import org.bukkit.ChatColor
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.enchantments.EnchantmentTarget
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.acrylicstyle.customenchantments.CustomEnchantmentsPlugin
import xyz.acrylicstyle.customenchantments.api.enchantment.ActivateType
import xyz.acrylicstyle.customenchantments.api.enchantment.CustomEnchantment

class SpeedEnchant : CustomEnchantment(NamespacedKey(CustomEnchantmentsPlugin.instance, "speed")) {
    override val name = "迅速"
    override val itemTarget = EnchantmentTarget.ARMOR_FEET
    override val maxLevel = 10

    override fun getDescription(level: Int): List<String> = listOf("移動する速度が", "${ChatColor.GREEN}+${level * 10}%${ChatColor.GRAY}上昇します。")

    override fun canEnchantItem(item: ItemStack): Boolean = item.type.name.endsWith("BOOTS")

    override fun canActivateEnchantment(type: ActivateType, item: ItemStack): Boolean =
        type == ActivateType.ARMOR_CHANGED && item.type.name.endsWith("BOOTS")

    override fun onActivate(player: Player, level: Int) {
        player.walkSpeed = player.walkSpeed + level * 0.02F
    }

    override fun onDeactivate(player: Player, level: Int) {
        player.walkSpeed = (player.walkSpeed - level * 0.02F).coerceAtLeast(0.2F)
    }

    override fun conflictsWith(other: Enchantment): Boolean = false
}
