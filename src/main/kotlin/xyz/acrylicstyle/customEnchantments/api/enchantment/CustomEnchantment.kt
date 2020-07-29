package xyz.acrylicstyle.customEnchantments.api.enchantment

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import util.CollectionList
import xyz.acrylicstyle.customEnchantments.api.CustomEnchantments
import xyz.acrylicstyle.tomeito_api.gui.PerPlayerInventory

abstract class CustomEnchantment(val id: String) : Enchantment(-1) {
    companion object {
        val activeEffects = PerPlayerInventory<CollectionList<Pair<CustomEnchantment, Int>>> { _ -> CollectionList() }

        fun deactivateAllActiveEffects(player: Player) {
            activeEffects[player.uniqueId].clone().forEach { pair ->
                pair.first.deactivate(player, pair.second)
            }
            activeEffects.get(player.uniqueId).clear()
        }
    }

    open fun getMaximumAnvilAbleLevel(): Int = maxLevel + 1

    abstract fun getDescription(): List<String>
    abstract override fun getName(): String // un-deprecate
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
    fun getEnchantmentBook(level: Int): ItemStack {
        val item = ItemStack(Material.ENCHANTED_BOOK)
        return CustomEnchantments.getInstance()
                .orElseThrow { IllegalStateException("CustomEnchantments plugin needs to be loaded to use this feature") }
                .getManager()
                .applyEnchantment(item, this, level)
    }
}
