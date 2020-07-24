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

class JumpEnchant : CustomEnchantment(NamespacedKey(CustomEnchantmentsPlugin.instance, "jump")) {
    override fun getDescription(): List<String> = listOf(ChatColor.YELLOW.toString() + "Increases jump boost by enchantment level.")

    override fun canEnchantItem(item: ItemStack): Boolean {
        return item.type.name.endsWith("BOOTS")
    }

    override fun getItemTarget(): EnchantmentTarget = EnchantmentTarget.ARMOR_FEET

    override fun getName(): String = "跳躍力上昇"

    override fun isCursed(): Boolean = false

    override fun onActivate(player: Player, level: Int) {
        val amplifier = player.getPotionEffect(PotionEffectType.JUMP)?.amplifier ?: 0
        player.addPotionEffect(PotionEffect(PotionEffectType.JUMP, Int.MAX_VALUE, amplifier + level - 1))
    }

    override fun onDeactivate(player: Player, level: Int) {
        player.removePotionEffect(PotionEffectType.JUMP)
    }

    override fun isTreasure(): Boolean = false

    override fun getMaxLevel(): Int = 5

    override fun getStartLevel(): Int = 1

    override fun conflictsWith(other: Enchantment): Boolean = false
}