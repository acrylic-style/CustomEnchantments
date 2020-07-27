package xyz.acrylicstyle.customEnchantments

import net.minecraft.server.v1_8_R3.NBTBase
import net.minecraft.server.v1_8_R3.NBTTagCompound
import net.minecraft.server.v1_8_R3.NBTTagList
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import util.Collection
import util.CollectionList
import xyz.acrylicstyle.customEnchantments.api.EnchantmentManager
import xyz.acrylicstyle.customEnchantments.api.enchantment.CustomEnchantment
import xyz.acrylicstyle.customEnchantments.api.enchantment.EnchantmentLevel
import java.util.concurrent.atomic.AtomicReference

open class EnchantmentManagerImpl(val plugin: CustomEnchantmentsPlugin) : EnchantmentManager {
    private val enchantments = CollectionList<CustomEnchantment>()
    private val enchantmentsMap = Collection<Class<out CustomEnchantment>, CustomEnchantment>()

    override fun registerEnchantment(enchantment: CustomEnchantment) {
        if (enchantments.filter { enchant -> enchant.id == enchantment.id }.isNotEmpty()) throw IllegalArgumentException("Duplicate enchantment key: " + enchantment.id)
        enchantmentsMap.add(enchantment.javaClass, enchantment)
        enchantments.add(enchantment)
    }

    override fun getEnchantments(): CollectionList<CustomEnchantment> = enchantments

    override fun getById(id: String): CustomEnchantment? = enchantments.filter { enchantment -> enchantment.id == id }.first()

    override fun applyEnchantment(item: ItemStack, enchantment: CustomEnchantment, level: Int): ItemStack {
        val meta = item.itemMeta
        val lore: CollectionList<String> = CollectionList(if (meta.hasLore()) meta.lore!! else CollectionList<String>())
        lore.add(ChatColor.GRAY.toString() + "${enchantment.name} " + ChatColor.GRAY + (EnchantmentLevel.getByLevel(level)?.name ?: level))
        meta.lore = lore
        if (!meta.hasEnchants()) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            meta.addEnchant(Enchantment.DAMAGE_ARTHROPODS, 1, true)
        }
        item.itemMeta = meta
        val itemStack = CraftItemStack.asNMSCopy(item)
        val tag = if (itemStack.hasTag()) itemStack.tag else NBTTagCompound()
        val storedEnchantments = (tag.get("storedEnchantments") ?: NBTTagList()) as NBTTagList
        val existingEnchantment = getStoredEnchantments(storedEnchantments).find { nbt -> (nbt as NBTTagCompound).getString("id").replace(".*:(.*)".toRegex(), "$1") == enchantment.id }
        if (existingEnchantment != null) getStoredEnchantments(storedEnchantments).remove(existingEnchantment)
        val enchantmentTag = NBTTagCompound()
        enchantmentTag.setString("id", enchantment.id)
        enchantmentTag.setInt("lvl", level)
        storedEnchantments.add(enchantmentTag)
        tag.set("storedEnchantments", storedEnchantments)
        itemStack.tag = tag
        return CraftItemStack.asBukkitCopy(itemStack)
    }

    private fun getStoredEnchantments(storedEnchantments: NBTTagList): MutableList<NBTBase> {
        val f = NBTTagList::class.java.getDeclaredField("list")
        f.isAccessible = true
        @Suppress("UNCHECKED_CAST") return f.get(storedEnchantments) as MutableList<NBTBase>
    }

    override fun removeEnchantment(item: ItemStack, enchantment: CustomEnchantment): ItemStack {
        val meta = item.itemMeta
        val lore: CollectionList<String> = CollectionList(if (meta.hasLore()) meta.lore!! else CollectionList<String>())
        val s = lore.find { s -> s.startsWith(ChatColor.GRAY.toString() + "${enchantment.name} ") }
        if (s != null) lore.remove(s)
        meta.lore = if (lore.isEmpty()) null else lore
        item.itemMeta = meta
        val itemStack = CraftItemStack.asNMSCopy(item)
        val tag = if (itemStack.hasTag()) itemStack.tag else NBTTagCompound()
        val storedEnchantments = (tag.get("storedEnchantments") ?: NBTTagList()) as NBTTagList
        val existingEnchantment = getStoredEnchantments(storedEnchantments).find { nbt -> (nbt as NBTTagCompound).getString("id").replace(".*:(.*)".toRegex(), "$1") == enchantment.id }
        if (existingEnchantment != null) getStoredEnchantments(storedEnchantments).remove(existingEnchantment)
        if (storedEnchantments.isEmpty) {
            tag.remove("storedEnchantments")
        } else {
            tag.set("storedEnchantments", storedEnchantments)
        }
        itemStack.tag = tag
        val i = CraftItemStack.asBukkitCopy(itemStack)
        val meta2 = i.itemMeta
        if (meta2.enchants.size > 1 || storedEnchantments.isEmpty) {
            meta2.removeItemFlags(ItemFlag.HIDE_ENCHANTS)
            if (meta2.getEnchantLevel(Enchantment.DAMAGE_ARTHROPODS) == 1) meta2.removeEnchant(Enchantment.DAMAGE_ARTHROPODS)
            if (meta2.getEnchantLevel(Enchantment.DAMAGE_ARTHROPODS) == 65534) meta2.removeEnchant(Enchantment.DAMAGE_ARTHROPODS)
        }
        i.itemMeta = meta2
        return i
    }

    override fun removeEnchantments(item: ItemStack): ItemStack {
        val i = AtomicReference(item)
        getEnchantments(item).forEach { ench -> i.set(removeEnchantment(i.get(), ench)) }
        return i.get()
    }

    override fun hasEnchantment(item: ItemStack, enchantment: CustomEnchantment): Boolean {
        val itemStack = CraftItemStack.asNMSCopy(item)
        val tag = if (itemStack.hasTag()) itemStack.tag else NBTTagCompound()
        val storedEnchantments = (tag.get("storedEnchantments") ?: NBTTagList()) as NBTTagList
        val existingEnchantment = getStoredEnchantments(storedEnchantments).find { nbt ->
            (nbt as NBTTagCompound).getString("id").replace(".*:(.*)".toRegex(), "$1") == enchantment.id
                    && (!nbt.getString("id").contains(":") || nbt.getString("id").replace("(.*):.*".toRegex(), "$1") == enchantment.id)
        }
        return existingEnchantment != null
    }

    override fun getEnchantments(item: ItemStack?): CollectionList<CustomEnchantment> {
        if (item == null || item.type == Material.AIR) return CollectionList()
        val itemStack = CraftItemStack.asNMSCopy(item)
        val tag = if (itemStack.hasTag()) itemStack.tag else NBTTagCompound()
        val storedEnchantments = (tag.get("storedEnchantments") ?: NBTTagList()) as NBTTagList
        val enchantments = CollectionList<CustomEnchantment>()
        getStoredEnchantments(storedEnchantments).forEach { nbt ->
            val t = nbt as NBTTagCompound
            enchantments.add(getById(t.getString("id")))
        }
        return enchantments
    }

    override fun hasEnchantmentOfLevel(item: ItemStack, enchantment: CustomEnchantment, level: Int): Boolean {
        val itemStack = CraftItemStack.asNMSCopy(item)
        val tag = if (itemStack.hasTag()) itemStack.tag else NBTTagCompound()
        val storedEnchantments = (tag.get("storedEnchantments") ?: NBTTagList()) as NBTTagList
        val existingEnchantment = getStoredEnchantments(storedEnchantments).find { nbt ->
            (nbt as NBTTagCompound).getString("id").replace(".*:(.*)".toRegex(), "$1") == enchantment.id
        } ?: return false
        val e = (existingEnchantment as NBTTagCompound)
        return e.getInt("lvl") == level
    }

    override fun getEnchantmentLevel(item: ItemStack?, enchantment: CustomEnchantment): Int {
        if (item == null) return 0
        val itemStack = CraftItemStack.asNMSCopy(item)
        val tag = if (itemStack.hasTag()) itemStack.tag else NBTTagCompound()
        val storedEnchantments = (tag.get("storedEnchantments") ?: NBTTagList()) as NBTTagList
        val existingEnchantment = getStoredEnchantments(storedEnchantments).find { nbt ->
            (nbt as NBTTagCompound).getString("id").replace(".*:(.*)".toRegex(), "$1") == enchantment.id
        } ?: return 0
        val e = (existingEnchantment as NBTTagCompound)
        return e.getInt("lvl")
    }

    override fun hasEnchantments(item: ItemStack?): Boolean = getEnchantments(item).isNotEmpty()

    override fun getEnchantment(clazz: Class<out CustomEnchantment>): CustomEnchantment? = enchantmentsMap[clazz]
}
