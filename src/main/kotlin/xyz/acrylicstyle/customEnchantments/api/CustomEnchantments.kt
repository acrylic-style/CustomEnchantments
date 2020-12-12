package xyz.acrylicstyle.customEnchantments.api

import org.bukkit.Bukkit
import java.util.*

interface CustomEnchantments {
    companion object {
        fun getInstance(): Optional<CustomEnchantments> {
            val plugin = Bukkit.getPluginManager().getPlugin("CustomEnchantments")
            if (plugin == null || plugin !is CustomEnchantments) return Optional.empty()
            return Optional.of(plugin)
        }
    }

    fun getManager(): EnchantmentManager
}
