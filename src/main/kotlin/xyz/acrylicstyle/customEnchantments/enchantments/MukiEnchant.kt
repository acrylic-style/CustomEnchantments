package xyz.acrylicstyle.customEnchantments.enchantments

import org.bukkit.ChatColor
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.enchantments.EnchantmentTarget
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.acrylicstyle.customEnchantments.CustomEnchantmentsPlugin
import xyz.acrylicstyle.customEnchantments.api.enchantment.CustomEnchantment

class MukiEnchant : CustomEnchantment(NamespacedKey(CustomEnchantmentsPlugin.instance, "muki1574")) {
    override fun getDescription(level: Int): List<String> = listOf("muki1574を攻撃するときに", "${ChatColor.GREEN}${1000 * level}%${ChatColor.GRAY}のダメージを与えます。")

    override fun canEnchantItem(item: ItemStack): Boolean {
        return item.type.name.endsWith("SWORD")
    }

    override fun getItemTarget(): EnchantmentTarget = EnchantmentTarget.WEAPON

    override fun getName(): String = "muki特攻"

    override fun isCursed(): Boolean = false

    override fun isTreasure(): Boolean = false

    override fun getMaximumAnvilableLevel(): Int = 1

    override fun getMaxLevel(): Int = 1

    override fun getStartLevel(): Int = 1

    override fun conflictsWith(other: Enchantment): Boolean = false
}