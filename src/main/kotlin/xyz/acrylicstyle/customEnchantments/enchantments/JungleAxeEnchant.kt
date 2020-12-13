package xyz.acrylicstyle.customEnchantments.enchantments

import org.bukkit.ChatColor
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.enchantments.EnchantmentTarget
import org.bukkit.inventory.ItemStack
import xyz.acrylicstyle.customEnchantments.CustomEnchantmentsPlugin
import xyz.acrylicstyle.customEnchantments.api.enchantment.CustomEnchantment

class JungleAxeEnchant : CustomEnchantment(NamespacedKey(CustomEnchantmentsPlugin.instance, "jungle_axe")) {
    override fun getDescription(level: Int): List<String> = listOf("原木を伐採するときに", "最大${ChatColor.GREEN}${level * 10}ブロック${ChatColor.GRAY}までを自動で伐採します。", "また、効率強化で伐採する速度が", "${ChatColor.GREEN}10%${ChatColor.GRAY}ずつ上がります。(最大${ChatColor.GREEN}100%${ChatColor.GRAY})")

    override fun canEnchantItem(item: ItemStack): Boolean {
        return item.type.name.endsWith("AXE")
    }

    override fun getItemTarget(): EnchantmentTarget = EnchantmentTarget.TOOL

    override fun getName(): String = "ジャングルの斧"

    override fun isCursed(): Boolean = false

    override fun isTreasure(): Boolean = false

    override fun getMaximumAnvilableLevel(): Int = 15

    override fun getMaxLevel(): Int = 15

    override fun getStartLevel(): Int = 1

    override fun conflictsWith(other: Enchantment): Boolean = false
}