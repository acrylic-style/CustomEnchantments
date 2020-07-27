package xyz.acrylicstyle.customEnchantments.nms

import net.minecraft.server.v1_8_R3.MojangsonParser
import net.minecraft.server.v1_8_R3.NBTTagCompound
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack
import org.bukkit.inventory.ItemStack

class MojangsonParser {
    companion object {
        private fun parse(json: String): NBTTagCompound = MojangsonParser.parse(json)

        fun combine(itemStack: ItemStack, json: String): ItemStack {
            val util = CraftItemStack.asNMSCopy(itemStack)
            util.tag = parse(json)
            return CraftItemStack.asBukkitCopy(util)
        }
    }
}
