package xyz.acrylicstyle.customEnchantments

import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import util.Collection
import util.CollectionList
import util.ICollectionList
import xyz.acrylicstyle.customEnchantments.api.EnchantmentManager
import xyz.acrylicstyle.customEnchantments.api.enchantment.CustomEnchantedData
import xyz.acrylicstyle.customEnchantments.api.enchantment.CustomEnchantment
import xyz.acrylicstyle.customEnchantments.api.enchantment.EnchantmentLevel
import xyz.acrylicstyle.paper.Paper
import xyz.acrylicstyle.paper.nbt.NBTTagCompound
import xyz.acrylicstyle.paper.nbt.NBTTagList
import xyz.acrylicstyle.tomeito_api.utils.Log
import java.util.concurrent.atomic.AtomicBoolean

open class EnchantmentManagerImpl(val plugin: CustomEnchantmentsPlugin) : EnchantmentManager {
    companion object {
        fun Location.getNearbyBlocks(radius: Int): CollectionList<Block> {
            val blocks = CollectionList<Block>()
            for (x in this.blockX - radius..this.blockX + radius) {
                for (y in this.blockY - radius..this.blockY + radius) {
                    for (z in this.blockZ - radius..this.blockZ + radius) {
                        blocks.add(this.world.getBlockAt(x, y, z))
                    }
                }
            }
            return blocks
        }
    }

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
        if (lore.anyMatch { s -> s.startsWith("${ChatColor.BLUE}${enchantment.name} ") }) {
            lore.removeAll(findLore(lore, enchantment) as kotlin.collections.Collection<String>)
        }
        lore.add(
            "${ChatColor.BLUE}${enchantment.name} ${ChatColor.BLUE}" + (EnchantmentLevel.getByLevel(level)?.name
                ?: level) + (if (anti) " ${ChatColor.RED}(Anti)" else "")
        )
        if (enchantment.getDescription(level).isNotEmpty()) lore.addAll(
            ICollectionList.asList(enchantment.getDescription(level))
                .map { s -> "${ChatColor.GRAY}$s" } as kotlin.collections.Collection<String>)
        lore.add(" ")
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

    private fun findLore(list: CollectionList<String>, enchantment: CustomEnchantment): CollectionList<String> {
        val lst = CollectionList<String>()
        val lookAhead = AtomicBoolean()
        list.forEach { s ->
            if (s.startsWith("${ChatColor.GRAY}${enchantment.name} ")) {
                lst.add(s)
                return@forEach
            }
            if (s.startsWith("${ChatColor.BLUE}${enchantment.name} ")) {
                lst.add(s)
                lookAhead.set(true)
            }
            if (!lookAhead.get()) return@forEach
            if (lst.isNotEmpty()) {
                lst.add(s)
                if (s == " ") lookAhead.set(false)
            }
        }
        return lst
    }

    override fun removeEnchantment(item: ItemStack, enchantment: CustomEnchantment): ItemStack {
        val meta = item.itemMeta
        val lore: CollectionList<String> = CollectionList(if (meta.hasLore()) meta.lore!! else CollectionList<String>())
        lore.removeAll(findLore(lore, enchantment))
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
            enchantments.add(CustomEnchantedData(ench, t.getInt("lvl"), t.getBoolean("anti")))
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

    override fun getEnchantment(clazz: Class<out CustomEnchantment>): CustomEnchantment? = enchantmentsMap[clazz]
}
