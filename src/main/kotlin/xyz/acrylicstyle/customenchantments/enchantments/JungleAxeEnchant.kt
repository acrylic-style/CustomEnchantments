package xyz.acrylicstyle.customenchantments.enchantments

import org.bukkit.ChatColor
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.enchantments.EnchantmentTarget
import org.bukkit.inventory.ItemStack
import xyz.acrylicstyle.customenchantments.CustomEnchantmentsPlugin
import xyz.acrylicstyle.customenchantments.api.enchantment.CustomEnchantment

class JungleAxeEnchant : CustomEnchantment(NamespacedKey(CustomEnchantmentsPlugin.instance, "jungle_axe")) {
    override val name = "ジャングルの斧"
    override val itemTarget = EnchantmentTarget.TOOL
    override val maxLevel = 15

    override fun getDescription(level: Int): List<String> = listOf("原木を伐採するときに", "最大${ChatColor.GREEN}${level * 10}ブロック${ChatColor.GRAY}までを自動で伐採します。", "また、効率強化で伐採する速度が", "${ChatColor.GREEN}10%${ChatColor.GRAY}ずつ上がります。(最大${ChatColor.GREEN}100%${ChatColor.GRAY})")

    override fun canEnchantItem(item: ItemStack): Boolean {
        return item.type.name.endsWith("AXE")
    }

    override fun getMaximumAnvilableLevel(): Int = 15

    override fun conflictsWith(other: Enchantment): Boolean = false
}
