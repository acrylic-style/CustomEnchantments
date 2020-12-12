package xyz.acrylicstyle.customEnchantments

import org.bukkit.ChatColor
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import util.Collection
import util.CollectionList
import xyz.acrylicstyle.customEnchantments.api.EnchantmentManager
import xyz.acrylicstyle.customEnchantments.api.enchantment.CustomEnchantedData
import xyz.acrylicstyle.customEnchantments.api.enchantment.CustomEnchantment
import xyz.acrylicstyle.customEnchantments.api.enchantment.EnchantmentLevel
import xyz.acrylicstyle.paper.Paper
import xyz.acrylicstyle.paper.nbt.NBTTagCompound
import xyz.acrylicstyle.paper.nbt.NBTTagList
import xyz.acrylicstyle.tomeito_api.utils.Log

open class EnchantmentManagerImpl(val plugin: CustomEnchantmentsPlugin) : EnchantmentManager {
    private val enchantments = CollectionList<CustomEnchantment>()
    private val enchantmentsMap = Collection<Class<out CustomEnchantment>, CustomEnchantment>()

    override fun registerEnchantment(enchantment: CustomEnchantment) {
        if (enchantments.filter { enchant -> enchant.key == enchantment.key }.isNotEmpty()) throw IllegalArgumentException("Duplicate enchantment key: " + enchantment.key.toString())
        enchantmentsMap.add(enchantment.javaClass, enchantment)
        enchantments.add(enchantment)
    }

    override fun getEnchantments(): CollectionList<CustomEnchantment> = enchantments

    override fun getById(id: String): CustomEnchantment? {
        val pluginId = if (id.contains(":")) id.replace("(.*):.*".toRegex(), "$1") else null
        val name = id.replace(".*:(.*)".toRegex(), "$1")
        return enchantments.filter { enchantment ->
            val key = enchantment.key.toString()
            if (pluginId == null) enchantment.key.key == name else key == "$pluginId:$name"
        }.first()
    }

    override fun applyEnchantment(item: ItemStack, enchantment: CustomEnchantment, level: Int, anti: Boolean): ItemStack {
        val meta = item.itemMeta
        val lore: CollectionList<String> = CollectionList(if (meta.hasLore()) meta.lore!! else CollectionList<String>())
        lore.add("${ChatColor.GRAY}${enchantment.name} ${ChatColor.GRAY}" + (EnchantmentLevel.getByLevel(level)?.name ?: level) + (if (anti) " ${ChatColor.RED}(Anti)" else ""))
        meta.lore = lore
        if (!meta.hasEnchants()) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            meta.addEnchant(Enchantment.DAMAGE_ARTHROPODS, 1, true)
        }
        item.itemMeta = meta
        val itemStack = Paper.itemStack(item)
        val tag = itemStack.orCreateTag
        val storedEnchantments = (tag.get("storedEnchantments") ?: NBTTagList()) as NBTTagList
        val existingEnchantment = storedEnchantments.find { nbt ->
            (nbt as NBTTagCompound).getString("id").replace(".*:(.*)".toRegex(), "$1") == enchantment.key.key
                    && (!nbt.getString("id").contains(":") || nbt.getString("id").replace("(.*):.*".toRegex(), "$1") == enchantment.key.namespace)
        }
        if (existingEnchantment != null) storedEnchantments.remove(existingEnchantment)
        val enchantmentTag = NBTTagCompound()
        enchantmentTag.setString("id", enchantment.key.toString())
        enchantmentTag.setInt("lvl", level)
        enchantmentTag.setBoolean("anti", anti)
        storedEnchantments.add(enchantmentTag)
        tag.set("storedEnchantments", storedEnchantments)
        itemStack.tag = tag
        return itemStack.itemStack
    }

    override fun removeEnchantment(item: ItemStack, enchantment: CustomEnchantment): ItemStack {
        val meta = item.itemMeta
        val lore: CollectionList<String> = CollectionList(if (meta.hasLore()) meta.lore!! else CollectionList<String>())
        val s = lore.find { s -> s.startsWith(ChatColor.GRAY.toString() + "${enchantment.name} ") }
        if (s != null) lore.remove(s)
        meta.lore = if (lore.isEmpty()) null else lore
        item.itemMeta = meta
        val itemStack = Paper.itemStack(item)
        val tag = itemStack.orCreateTag
        val storedEnchantments = (tag.get("storedEnchantments") ?: NBTTagList()) as NBTTagList
        val existingEnchantment = storedEnchantments.find { nbt ->
            (nbt as NBTTagCompound).getString("id").replace(".*:(.*)".toRegex(), "$1") == enchantment.key.key
                    && (!nbt.getString("id").contains(":") || nbt.getString("id").replace("(.*):.*".toRegex(), "$1") == enchantment.key.namespace)
        }
        if (existingEnchantment != null) storedEnchantments.remove(existingEnchantment)
        if (storedEnchantments.isEmpty()) {
            tag.remove("storedEnchantments")
        } else {
            tag.set("storedEnchantments", storedEnchantments)
        }
        itemStack.tag = tag
        val i = itemStack.itemStack
        val meta2 = i.itemMeta
        if (storedEnchantments.isEmpty()) {
            meta2.removeItemFlags(ItemFlag.HIDE_ENCHANTS)
            if (meta2.getEnchantLevel(Enchantment.DAMAGE_ARTHROPODS) == 1) meta2.removeEnchant(Enchantment.DAMAGE_ARTHROPODS)
            if (meta2.getEnchantLevel(Enchantment.DAMAGE_ARTHROPODS) == 65534) meta2.removeEnchant(Enchantment.DAMAGE_ARTHROPODS)
        }
        i.itemMeta = meta2
        return i
    }

    override fun removeEnchantments(item: ItemStack): ItemStack {
        val i = Paper.itemStack(item)
        val tag = i.orCreateTag
        tag.remove("storedEnchantments")
        i.tag = tag
        return i.itemStack
    }

    override fun hasEnchantment(item: ItemStack, enchantment: CustomEnchantment): Boolean {
        val itemStack = Paper.itemStack(item)
        val tag = itemStack.orCreateTag
        val storedEnchantments = (tag.get("storedEnchantments") ?: NBTTagList()) as NBTTagList
        val existingEnchantment = storedEnchantments.find { nbt ->
            (nbt as NBTTagCompound).getString("id").replace(".*:(.*)".toRegex(), "$1") == enchantment.key.key
                    && (!nbt.getString("id").contains(":") || nbt.getString("id").replace("(.*):.*".toRegex(), "$1") == enchantment.key.namespace)
        }
        return existingEnchantment != null
    }

    override fun getEnchantments(item: ItemStack?): CollectionList<CustomEnchantedData> {
        if (item == null) return CollectionList()
        val itemStack = Paper.itemStack(item)
        val tag = itemStack.orCreateTag
        val storedEnchantments = (tag.get("storedEnchantments") ?: NBTTagList()) as NBTTagList
        val enchantments = CollectionList<CustomEnchantedData>()
        storedEnchantments.forEach { nbt ->
            val t = nbt as NBTTagCompound
            val ench = getById(t.getString("id"))
            if (ench == null) {
                Log.with("CustomEnchantments").warn("Skipping enchantment ${t.getString("id")}")
                return@forEach
            }
            enchantments.add(CustomEnchantedData(ench, t.getBoolean("anti")))
        }
        return enchantments
    }

    override fun hasEnchantmentOfLevel(item: ItemStack, enchantment: CustomEnchantment, level: Int): Boolean {
        val itemStack = Paper.itemStack(item)
        val tag = itemStack.orCreateTag
        val storedEnchantments = (tag.get("storedEnchantments") ?: NBTTagList()) as NBTTagList
        val existingEnchantment = storedEnchantments.find { nbt ->
            (nbt as NBTTagCompound).getString("id").replace(".*:(.*)".toRegex(), "$1") == enchantment.key.key
                    && (!nbt.getString("id").contains(":") || nbt.getString("id").replace("(.*):.*".toRegex(), "$1") == enchantment.key.namespace)
        } ?: return false
        val e = (existingEnchantment as NBTTagCompound)
        return e.getInt("lvl") == level
    }

    override fun getEnchantmentLevel(item: ItemStack?, enchantment: CustomEnchantment): Int {
        if (item == null) return 0
        val itemStack = Paper.itemStack(item)
        val tag = itemStack.orCreateTag
        val storedEnchantments = (tag.get("storedEnchantments") ?: NBTTagList()) as NBTTagList
        val existingEnchantment = storedEnchantments.find { nbt ->
            (nbt as NBTTagCompound).getString("id").replace(".*:(.*)".toRegex(), "$1") == enchantment.key.key
                    && (!nbt.getString("id").contains(":") || nbt.getString("id").replace("(.*):.*".toRegex(), "$1") == enchantment.key.namespace)
        } ?: return 0
        val e = (existingEnchantment as NBTTagCompound)
        return e.getInt("lvl")
    }

    override fun hasEnchantments(item: ItemStack?): Boolean = getEnchantments(item).isNotEmpty()

    override fun getEnchantment(clazz: Class<out CustomEnchantment>): CustomEnchantment? = enchantmentsMap.get(clazz)
}
