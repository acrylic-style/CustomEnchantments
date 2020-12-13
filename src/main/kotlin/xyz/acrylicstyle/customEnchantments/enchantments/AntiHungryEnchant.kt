package xyz.acrylicstyle.customEnchantments.enchantments

import org.bukkit.ChatColor
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.enchantments.EnchantmentTarget
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import xyz.acrylicstyle.customEnchantments.CustomEnchantmentsPlugin
import xyz.acrylicstyle.customEnchantments.api.enchantment.CustomEnchantment

class AntiHungryEnchant : CustomEnchantment(NamespacedKey(CustomEnchantmentsPlugin.instance, "anti_hungry")) {
    override fun getDescription(level: Int): List<String> = listOf("満腹度回復${ChatColor.GREEN}$level${ChatColor.GRAY}を付与します。")

    override fun canEnchantItem(item: ItemStack): Boolean {
        val s = item.type.name
        return s.endsWith("BOOTS") || s.endsWith("CHESTPLATE") || s.endsWith("LEGGINGS") || s.endsWith("HELMET")
    }

    override fun getItemTarget(): EnchantmentTarget = EnchantmentTarget.ARMOR

    override fun getName(): String = "満腹度回復"

    override fun isCursed(): Boolean = false

    override fun onActivate(player: Player, level: Int) {
        player.addPotionEffect(PotionEffect(PotionEffectType.SATURATION, Int.MAX_VALUE, level-1))
    }

    override fun onDeactivate(player: Player, level: Int) {
        player.removePotionEffect(PotionEffectType.SATURATION)
    }

    override fun isTreasure(): Boolean = false

    override fun getMaximumAnvilableLevel(): Int = 1

    override fun getMaxLevel(): Int = 1

    override fun getStartLevel(): Int = 1

    override fun conflictsWith(other: Enchantment): Boolean = false
}