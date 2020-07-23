package xyz.acrylicstyle.customEnchantments.api

import org.bukkit.inventory.ItemStack
import util.CollectionList
import xyz.acrylicstyle.customEnchantments.api.enchantment.CustomEnchantment

interface EnchantmentManager {
    fun registerEnchantment(enchantment: CustomEnchantment)
    fun getEnchantments(): CollectionList<CustomEnchantment>
    fun getById(id: String): CustomEnchantment?
    fun applyEnchantment(item: ItemStack, enchantment: CustomEnchantment, level: Int): ItemStack
    fun removeEnchantment(item: ItemStack, enchantment: CustomEnchantment): ItemStack
    fun removeEnchantments(item: ItemStack): ItemStack
    fun hasEnchantment(item: ItemStack, enchantment: CustomEnchantment): Boolean
    fun hasEnchantmentOfLevel(item: ItemStack, enchantment: CustomEnchantment, level: Int): Boolean
    fun getEnchantments(item: ItemStack?): CollectionList<CustomEnchantment>
    fun getEnchantmentLevel(item: ItemStack?, enchantment: CustomEnchantment): Int
    fun hasEnchantments(item: ItemStack?): Boolean
}