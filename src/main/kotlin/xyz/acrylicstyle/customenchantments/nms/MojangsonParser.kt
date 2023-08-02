package xyz.acrylicstyle.customenchantments.nms

import net.azisaba.kotlinnmsextension.v1_20_R1.getOrCreateTag
import net.azisaba.kotlinnmsextension.v1_20_R1.tag
import net.minecraft.nbt.MojangsonParser
import net.minecraft.nbt.NBTTagCompound
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack
import org.bukkit.inventory.ItemStack

object MojangsonParser {
    @JvmStatic
    fun parse(json: String): NBTTagCompound {
        return MojangsonParser.a(json)
    }

    @JvmStatic
    fun combine(itemStack: ItemStack, json: String): ItemStack {
        val nms = CraftItemStack.asNMSCopy(itemStack)
        val tag = nms.getOrCreateTag()
        tag.a(parse(json))
        nms.tag = tag
        return CraftItemStack.asBukkitCopy(nms)
    }
}
