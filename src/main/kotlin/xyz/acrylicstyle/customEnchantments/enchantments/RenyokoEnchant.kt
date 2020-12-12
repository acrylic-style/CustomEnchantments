package xyz.acrylicstyle.customEnchantments.enchantments

import org.bukkit.ChatColor
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.enchantments.EnchantmentTarget
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.acrylicstyle.customEnchantments.CustomEnchantmentsPlugin
import xyz.acrylicstyle.customEnchantments.api.enchantment.CustomEnchantment

class RenyokoEnchant : CustomEnchantment(NamespacedKey(CustomEnchantmentsPlugin.instance, "renyoko")) {
    override fun getDescription(): List<String> = listOf(ChatColor.YELLOW.toString() + "Increases the damage to 1000% when attacking renyoko.")

    override fun canEnchantItem(item: ItemStack): Boolean {
        return item.type.name.endsWith("SWORD")
    }

    override fun getItemTarget(): EnchantmentTarget = EnchantmentTarget.WEAPON

    override fun getName(): String = "renyoko特攻"

    override fun isCursed(): Boolean = false

    override fun isTreasure(): Boolean = false

    override fun getMaximumAnvilableLevel(): Int = 1

    override fun getMaxLevel(): Int = 1

    override fun getStartLevel(): Int = 1

    override fun conflictsWith(other: Enchantment): Boolean = false
}