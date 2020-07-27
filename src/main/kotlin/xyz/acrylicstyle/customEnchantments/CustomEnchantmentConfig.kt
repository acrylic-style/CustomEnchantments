package xyz.acrylicstyle.customEnchantments

import org.bukkit.Material
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack
import org.bukkit.inventory.ItemStack
import xyz.acrylicstyle.customEnchantments.nms.MojangsonParser
import xyz.acrylicstyle.tomeito_api.providers.ConfigProvider

class CustomEnchantmentConfig : ConfigProvider("./plugins/CustomEnchantments/config.yml") {
    fun getItem(path: String): ItemStack? {
        val rawMaterial = this.getString("${path}.material") ?: return null
        val amount = this.getInt("${path}.amount", 1).coerceAtLeast(1)
        val rawNbt = this.getString("${path}.data")
        val material = Material.getMaterial(rawMaterial.toUpperCase()) ?: return null
        val item = ItemStack(material, amount)
        return if (rawNbt == null) item else MojangsonParser.combine(item, rawNbt)
    }

    fun setItem(path: String, item: ItemStack?) {
        if (item == null) return this.set(path, null)
        this.set("${path}.material", item.type)
        this.set("${path}.amount", item.amount)
        this.set("${path}.data", CraftItemStack.asNMSCopy(item).tag?.toString())
    }
}
