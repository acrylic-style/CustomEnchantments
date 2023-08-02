package xyz.acrylicstyle.customenchantments.api

import org.bukkit.inventory.ItemStack
import xyz.acrylicstyle.customenchantments.api.enchantment.CustomEnchantedData
import xyz.acrylicstyle.customenchantments.api.enchantment.CustomEnchantment

interface EnchantmentManager {
    fun registerEnchantment(enchantment: CustomEnchantment)
    fun getEnchantments(): List<CustomEnchantment>
    fun getById(id: String): CustomEnchantment?
    fun applyEnchantment(item: ItemStack, enchantment: CustomEnchantment, level: Int, anti: Boolean): ItemStack
    fun removeEnchantment(item: ItemStack, enchantment: CustomEnchantment): ItemStack
    fun removeEnchantments(item: ItemStack): ItemStack
    fun hasEnchantment(item: ItemStack, enchantment: CustomEnchantment): Boolean
    fun hasEnchantmentOfLevel(item: ItemStack, enchantment: CustomEnchantment, level: Int): Boolean
    fun getEnchantments(item: ItemStack?): List<CustomEnchantedData>
    fun getEnchantmentLevel(item: ItemStack?, enchantment: CustomEnchantment): Int
    fun hasEnchantments(item: ItemStack?): Boolean
    fun getEnchantment(clazz: Class<out CustomEnchantment>): CustomEnchantment?
}
