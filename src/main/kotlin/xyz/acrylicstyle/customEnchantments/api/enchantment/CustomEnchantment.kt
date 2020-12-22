package xyz.acrylicstyle.customEnchantments.api.enchantment

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.enchantments.EnchantmentTarget
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import util.CollectionList
import xyz.acrylicstyle.customEnchantments.api.CustomEnchantments
import xyz.acrylicstyle.tomeito_api.gui.PerPlayerInventory
import xyz.acrylicstyle.tomeito_api.utils.Log

abstract class CustomEnchantment(id: NamespacedKey) : Enchantment(id) {
    companion object {
        val activeEffects = PerPlayerInventory<CollectionList<Pair<CustomEnchantment, Int>>> { _ -> CollectionList() }

        fun deactivateAllActiveEffects(player: Player) {
            activeEffects.get(player.uniqueId).clone().forEach { pair ->
                pair.first.deactivate(player, pair.second)
            }
            activeEffects.get(player.uniqueId).clear()
        }
    }

    open fun getMaximumAnvilableLevel(): Int = maxLevel + 1
    open fun canActivateEnchantment(type: ActivateType, item: ItemStack): Boolean {
        if (item.type == Material.ENCHANTED_BOOK) return false // enchanted books cannot activate enchantments
        return if (type == ActivateType.ARMOR_CHANGED) {
            itemTarget.name.startsWith("ARMOR") || itemTarget == EnchantmentTarget.WEARABLE
        } else if (type == ActivateType.ITEM_HELD) {
            !itemTarget.name.startsWith("ARMOR") && itemTarget != EnchantmentTarget.WEARABLE
        } else {
            Log.with("CustomEnchantments").warn("Unknown ActivateType: ${type.name}")
            true // :thinking:
        }
    }

    abstract fun getDescription(level: Int): List<String>
    abstract override fun getName(): String // un-deprecate, remove 'override' modifier when it was removed on Enchantment class
    abstract override fun isCursed(): Boolean // un-deprecate
    protected open fun onActivate(player: Player, level: Int) {}
    protected open fun onDeactivate(player: Player, level: Int) {}

    fun activate(player: Player, level: Int) {
        activeEffects.get(player.uniqueId).add(Pair(this, level))
        onActivate(player, level)
    }

    fun deactivate(player: Player, level: Int) {
        activeEffects.get(player.uniqueId).remove(Pair(this, level))
        onDeactivate(player, level)
    }

    /**
     * Get the enchantment book item.
     * @throws IllegalStateException when CustomEnchantments plugin isn't loaded
     */
    fun getEnchantmentBook(level: Int, anti: Boolean): ItemStack {
        val item = ItemStack(Material.ENCHANTED_BOOK)
        return CustomEnchantments.getInstance()
                .orElseThrow { IllegalStateException("CustomEnchantments plugin needs to be loaded to use this feature") }
                .getManager()
                .applyEnchantment(item, this, level, anti)
    }
}