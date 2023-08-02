package xyz.acrylicstyle.customenchantments.enchantments

import org.bukkit.ChatColor
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.enchantments.EnchantmentTarget
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import xyz.acrylicstyle.customenchantments.CustomEnchantmentsPlugin
import xyz.acrylicstyle.customenchantments.api.enchantment.CustomEnchantment

class JumpEnchant : CustomEnchantment(NamespacedKey(CustomEnchantmentsPlugin.instance, "jump")) {
    override val name = "跳躍力上昇"
    override val itemTarget = EnchantmentTarget.ARMOR_FEET
    override val maxLevel = 5

    override fun getDescription(level: Int): List<String> = listOf("跳躍力上昇が", "${ChatColor.GREEN}+$level${ChatColor.GRAY}上昇します。")

    override fun canEnchantItem(item: ItemStack): Boolean {
        return item.type.name.endsWith("BOOTS")
    }

    override fun onActivate(player: Player, level: Int) {
        val amplifier = player.getPotionEffect(PotionEffectType.JUMP)?.amplifier ?: 0
        player.addPotionEffect(PotionEffect(PotionEffectType.JUMP, Int.MAX_VALUE, amplifier + level - 1))
    }

    override fun onDeactivate(player: Player, level: Int) {
        player.removePotionEffect(PotionEffectType.JUMP)
    }

    override fun conflictsWith(other: Enchantment): Boolean = false
}
