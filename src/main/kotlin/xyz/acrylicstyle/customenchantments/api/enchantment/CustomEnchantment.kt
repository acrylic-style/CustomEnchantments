package xyz.acrylicstyle.customenchantments.api.enchantment

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.enchantments.EnchantmentTarget
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.acrylicstyle.customenchantments.api.CustomEnchantments
import xyz.acrylicstyle.customenchantments.util.UUIDMap

abstract class CustomEnchantment(val key: NamespacedKey) {
    companion object {
        val activeEffects = UUIDMap<MutableList<Pair<CustomEnchantment, Int>>> { mutableListOf() }

        fun deactivateAllActiveEffects(player: Player) {
            activeEffects[player.uniqueId].toList().forEach { pair ->
                pair.first.deactivate(player, pair.second)
            }
            activeEffects[player.uniqueId].clear()
        }
    }

    abstract val name: String
    abstract val itemTarget: EnchantmentTarget
    open val isCursed: Boolean = false
    open val isTreasure: Boolean = false
    open val startLevel: Int = 1
    abstract val maxLevel: Int

    open fun getMaximumAnvilableLevel(): Int = maxLevel + 1
    open fun canActivateEnchantment(type: ActivateType, item: ItemStack): Boolean {
        if (item.type == Material.ENCHANTED_BOOK) return false // enchanted books cannot activate enchantments
        return when (type) {
            ActivateType.ARMOR_CHANGED -> itemTarget.name.startsWith("ARMOR") || itemTarget == EnchantmentTarget.WEARABLE
            ActivateType.ITEM_HELD -> !itemTarget.name.startsWith("ARMOR") && itemTarget != EnchantmentTarget.WEARABLE
        }
    }

    abstract fun getDescription(level: Int): List<String>
    abstract fun canEnchantItem(item: ItemStack): Boolean
    abstract fun conflictsWith(other: Enchantment): Boolean

    protected open fun onActivate(player: Player, level: Int) {}
    protected open fun onDeactivate(player: Player, level: Int) {}

    fun activate(player: Player, level: Int) {
        activeEffects[player.uniqueId].add(Pair(this, level))
        onActivate(player, level)
    }

    fun deactivate(player: Player, level: Int) {
        activeEffects[player.uniqueId].remove(Pair(this, level))
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
