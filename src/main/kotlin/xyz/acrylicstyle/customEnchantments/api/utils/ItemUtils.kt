package xyz.acrylicstyle.customEnchantments.api.utils

import org.bukkit.Material

object ItemUtils {
    fun isArmor(material: Material): Boolean {
        if (material.name.endsWith("HELMET")) return true
        if (material.name.endsWith("CHESTPLATE")) return true
        if (material.name.endsWith("LEGGINGS")) return true
        if (material.name.endsWith("BOOTS")) return true
        return when (material) {
            Material.ELYTRA -> true
            else -> false
        }
    }
}