package xyz.acrylicstyle.customenchantments.enchantments

import org.bukkit.ChatColor
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.enchantments.Enchantment
import org.bukkit.enchantments.EnchantmentTarget
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.acrylicstyle.customenchantments.CustomEnchantmentsPlugin
import xyz.acrylicstyle.customenchantments.api.enchantment.CustomEnchantment
import kotlin.math.max

class GrowthEnchant : CustomEnchantment(NamespacedKey(CustomEnchantmentsPlugin.instance, "growth")) {
    override val name = "体力上昇"
    override val itemTarget = EnchantmentTarget.ARMOR
    override val maxLevel = 5

    override fun getDescription(level: Int): List<String> = listOf("最大体力が", "${ChatColor.RED}${level / 2F}${ChatColor.RED}❤${ChatColor.GRAY}増加します。")

    override fun canEnchantItem(item: ItemStack): Boolean {
        val s = item.type.name
        return s.endsWith("BOOTS") || s.endsWith("CHESTPLATE") || s.endsWith("LEGGINGS") || s.endsWith("HELMET")
    }

    override fun onActivate(player: Player, level: Int) {
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.let { it.baseValue += level }
    }

    override fun onDeactivate(player: Player, level: Int) {
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.let { it.baseValue = max(it.baseValue - level, CustomEnchantmentsPlugin.instance.config.getDouble("min-health")) }
    }

    override fun conflictsWith(other: Enchantment): Boolean = false
}
