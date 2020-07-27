package xyz.acrylicstyle.customEnchantments.enchantments

import org.bukkit.ChatColor
import org.bukkit.enchantments.Enchantment
import org.bukkit.enchantments.EnchantmentTarget
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.acrylicstyle.customEnchantments.api.enchantment.CustomEnchantment
import kotlin.math.max

class GrowthEnchant : CustomEnchantment("growth") {
    override fun getDescription(): List<String> = listOf(ChatColor.YELLOW.toString() + "Increases health by 5 * enchantment level.")

    override fun canEnchantItem(item: ItemStack): Boolean {
        val s = item.type.name
        return s.endsWith("BOOTS") || s.endsWith("CHESTPLATE") || s.endsWith("LEGGINGS") || s.endsWith("HELMET")
    }

    override fun getItemTarget(): EnchantmentTarget = EnchantmentTarget.ARMOR

    override fun getName(): String = "体力上昇"

    override fun onActivate(player: Player, level: Int) {
        player.maxHealth = player.maxHealth + level
    }

    override fun onDeactivate(player: Player, level: Int) {
        player.maxHealth = max(player.maxHealth - level, 20.0)
    }

    override fun getMaxLevel(): Int = 5

    override fun getStartLevel(): Int = 1

    override fun conflictsWith(other: Enchantment): Boolean = false
}