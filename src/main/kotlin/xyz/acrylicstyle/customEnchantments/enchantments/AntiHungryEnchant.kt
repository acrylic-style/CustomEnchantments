package xyz.acrylicstyle.customEnchantments.enchantments

import org.bukkit.ChatColor
import org.bukkit.enchantments.Enchantment
import org.bukkit.enchantments.EnchantmentTarget
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import xyz.acrylicstyle.customEnchantments.api.enchantment.CustomEnchantment

class AntiHungryEnchant : CustomEnchantment("anti_hungry") {
    override fun getDescription(): List<String> = listOf(ChatColor.YELLOW.toString() + "Gives you saturation, the ultimate anti-hungry solution.")

    override fun canEnchantItem(item: ItemStack): Boolean {
        val s = item.type.name
        return s.endsWith("BOOTS") || s.endsWith("CHESTPLATE") || s.endsWith("LEGGINGS") || s.endsWith("HELMET")
    }

    override fun getItemTarget(): EnchantmentTarget = EnchantmentTarget.ARMOR

    override fun getName(): String = "満腹度回復"

    override fun onActivate(player: Player, level: Int) {
        player.addPotionEffect(PotionEffect(PotionEffectType.SATURATION, Int.MAX_VALUE, level-1))
    }

    override fun onDeactivate(player: Player, level: Int) {
        player.removePotionEffect(PotionEffectType.SATURATION)
    }

    override fun getMaxLevel(): Int = 5

    override fun getStartLevel(): Int = 1

    override fun conflictsWith(other: Enchantment): Boolean = false
}