package xyz.acrylicstyle.customEnchantments.nms

import org.bukkit.inventory.ItemStack
import util.ReflectionHelper
import xyz.acrylicstyle.paper.Paper
import xyz.acrylicstyle.paper.nbt.CraftNBT
import xyz.acrylicstyle.paper.nbt.NBTTagCompound
import xyz.acrylicstyle.shared.NMSAPI
import xyz.acrylicstyle.tomeito_api.utils.ReflectionUtil

class MojangsonParser : NMSAPI(null, "MojangsonParser") {
    companion object {
        private val CLASS: Class<*> = getClassWithoutException("MojangsonParser")
        fun parse(json: String): NBTTagCompound {
            return CraftNBT::class.java
                .getMethod("asBukkitCompound", ReflectionUtil.getNMSClass("NBTTagCompound"))
                .invoke(null, ReflectionHelper.invokeMethodWithoutException(CLASS, null, "parse", json)) as NBTTagCompound
        }

        fun combine(itemStack: ItemStack, json: String): ItemStack {
            val util = Paper.itemStack(itemStack)
            util.tag = parse(json)
            //Log.debug("Parsed NBT: " + Objects.requireNonNull(util.getTag()).toString());
            return util.itemStack
        }
    }
}