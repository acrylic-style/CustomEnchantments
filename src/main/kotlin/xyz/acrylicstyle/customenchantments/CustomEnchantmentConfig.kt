package xyz.acrylicstyle.customenchantments

import net.azisaba.kotlinnmsextension.v1_20_R1.tag
import org.bukkit.Material
import org.bukkit.configuration.MemoryConfiguration
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack
import org.bukkit.inventory.ItemStack
import xyz.acrylicstyle.customenchantments.nms.MojangsonParser

class CustomEnchantmentConfig(private val config: MemoryConfiguration) {
    fun getItem(path: String): ItemStack? {
        val rawMaterial = config.getString("${path}.material") ?: return null
        val amount = config.getInt("${path}.amount", 1).coerceAtLeast(1)
        val rawNbt = config.getString("${path}.data")
        val material = Material.getMaterial(rawMaterial.uppercase()) ?: return null
        val item = ItemStack(material, amount)
        return if (rawNbt == null) item else MojangsonParser.combine(item, rawNbt)
    }

    fun setItem(path: String, item: ItemStack?) {
        if (item == null) return config.set(path, null)
        config.set("${path}.material", item.type)
        config.set("${path}.amount", item.amount)
        config.set("${path}.data", CraftItemStack.asNMSCopy(item).tag?.toString())
    }
}
