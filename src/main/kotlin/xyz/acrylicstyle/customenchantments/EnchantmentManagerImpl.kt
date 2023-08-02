package xyz.acrylicstyle.customenchantments

import net.azisaba.kotlinnmsextension.v1_20_R1.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import xyz.acrylicstyle.customenchantments.api.EnchantmentManager
import xyz.acrylicstyle.customenchantments.api.enchantment.CustomEnchantedData
import xyz.acrylicstyle.customenchantments.api.enchantment.CustomEnchantment
import xyz.acrylicstyle.customenchantments.api.enchantment.EnchantmentLevel
import java.util.concurrent.atomic.AtomicBoolean

open class EnchantmentManagerImpl(val plugin: CustomEnchantmentsPlugin) : EnchantmentManager {
    companion object {
        fun Location.getNearbyBlocks(radius: Int): List<Block> {
            val blocks = mutableListOf<Block>()
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

    private val enchantments = mutableListOf<CustomEnchantment>()
    private val enchantmentsMap = mutableMapOf<Class<out CustomEnchantment>, CustomEnchantment>()

    override fun registerEnchantment(enchantment: CustomEnchantment) {
        if (enchantments.any { it.key == enchantment.key }) throw IllegalArgumentException("Duplicate enchantment key: " + enchantment.key.toString())
        enchantmentsMap[enchantment.javaClass] = enchantment
        enchantments.add(enchantment)
    }

    override fun getEnchantments(): List<CustomEnchantment> = enchantments

    override fun getById(id: String): CustomEnchantment? {
        val pluginId = if (id.contains(":")) id.replace("(.*):.*".toRegex(), "$1") else null
        val name = id.replace(".*:(.*)".toRegex(), "$1")
        return enchantments.firstOrNull { enchantment ->
            val key = enchantment.key.toString()
            if (pluginId == null) enchantment.key.key == name else key == "$pluginId:$name"
        }
    }

    override fun applyEnchantment(item: ItemStack, enchantment: CustomEnchantment, level: Int, anti: Boolean): ItemStack {
        val meta = item.itemMeta
        val lore: MutableList<Component> = if (meta.hasLore()) meta.lore()!!.toMutableList() else mutableListOf()
        if (lore.any { LegacyComponentSerializer.legacySection().serialize(it).startsWith("${ChatColor.BLUE}${enchantment.name} ") }) {
            findLoreToRemove(lore, enchantment).forEach { lore.remove(it) }
        }
        val levelString = (EnchantmentLevel.getByLevel(level)?.name ?: level).toString()
        lore.add(
            Component.text("${enchantment.name} $levelString", NamedTextColor.BLUE)
                .noItalic()
                .let {
                    if (anti) {
                        it.append(Component.text(" (Anti)", NamedTextColor.RED).noItalic())
                    } else {
                        it
                    }
                }
        )
//        lore.add(Component.text(enchantment.name, NamedTextColor.BLUE))
        if (enchantment.getDescription(level).isNotEmpty()) {
            lore.addAll(enchantment.getDescription(level).map { Component.text(it, NamedTextColor.GRAY).noItalic() })
        }
        lore.add(Component.empty())
        meta.lore(lore)
        if (!meta.hasEnchants()) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            meta.addEnchant(Enchantment.DAMAGE_ARTHROPODS, 0, true)
        }
        item.itemMeta = meta
        val itemStack = CraftItemStack.asNMSCopy(item)
        val tag = itemStack.getOrCreateTag()
        val storedEnchantments = (tag["storedEnchantments"] ?: NBTTagList()) as NBTTagList
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
        tag["storedEnchantments"] = storedEnchantments
        itemStack.tag = tag
        return CraftItemStack.asBukkitCopy(itemStack)
    }

    private fun findLoreToRemove(list: List<Component>, enchantment: CustomEnchantment): List<Component> {
        val lst = mutableListOf<Component>()
        val lookAhead = AtomicBoolean()
        list.forEach { component ->
            CustomEnchantmentsPlugin.instance.logger.info(component.textContent())
            if (component.textContent().startsWith("${enchantment.name} ") && component.color() == NamedTextColor.GRAY) {
                lst.add(component)
                return@forEach
            }
            if (component.textContent().startsWith("${enchantment.name} ") && component.color() == NamedTextColor.BLUE) {
                lst.add(component)
                lookAhead.set(true)
            }
            if (!lookAhead.get()) return@forEach
            if (lst.isNotEmpty()) {
                lst.add(component)
                if (component.textContent() == "") {
                    lookAhead.set(false)
                }
            }
        }
        return lst
    }

    override fun removeEnchantment(item: ItemStack, enchantment: CustomEnchantment): ItemStack {
        val meta = item.itemMeta
        val lore = if (meta.hasLore()) meta.lore()!!.toMutableList() else mutableListOf()
        findLoreToRemove(lore, enchantment).forEach { lore.remove(it) }
        meta.lore(if (lore.isEmpty()) null else lore)
        item.itemMeta = meta
        val itemStack = CraftItemStack.asNMSCopy(item)
        val tag = itemStack.getOrCreateTag()
        val storedEnchantments = (tag["storedEnchantments"] ?: NBTTagList()) as NBTTagList
        val existingEnchantment = storedEnchantments.find { nbt ->
            (nbt as NBTTagCompound).getString("id").replace(".*:(.*)".toRegex(), "$1") == enchantment.key.key
                    && (!nbt.getString("id").contains(":") || nbt.getString("id").replace("(.*):.*".toRegex(), "$1") == enchantment.key.namespace)
        }
        if (existingEnchantment != null) storedEnchantments.remove(existingEnchantment)
        if (storedEnchantments.isEmpty()) {
            tag.remove("storedEnchantments")
        } else {
            tag["storedEnchantments"] = storedEnchantments
        }
        itemStack.tag = tag
        val i = CraftItemStack.asBukkitCopy(itemStack)
        val meta2 = i.itemMeta
        if (storedEnchantments.isEmpty()) {
            meta2.removeItemFlags(ItemFlag.HIDE_ENCHANTS)
            if (meta2.getEnchantLevel(Enchantment.DAMAGE_ARTHROPODS) == 0) meta2.removeEnchant(Enchantment.DAMAGE_ARTHROPODS)
            if (meta2.getEnchantLevel(Enchantment.DAMAGE_ARTHROPODS) == 65534) meta2.removeEnchant(Enchantment.DAMAGE_ARTHROPODS)
        }
        i.itemMeta = meta2
        return i
    }

    override fun removeEnchantments(item: ItemStack): ItemStack {
        val i = CraftItemStack.asNMSCopy(item)
        val tag = i.getOrCreateTag()
        tag.remove("storedEnchantments")
        i.tag = if (tag.size == 0) null else tag
        return CraftItemStack.asBukkitCopy(i)
    }

    override fun hasEnchantment(item: ItemStack, enchantment: CustomEnchantment): Boolean {
        val itemStack = CraftItemStack.asNMSCopy(item)
        val tag = itemStack.getOrCreateTag()
        val storedEnchantments = (tag["storedEnchantments"] ?: NBTTagList()) as NBTTagList
        val existingEnchantment = storedEnchantments.find { nbt ->
            (nbt as NBTTagCompound).getString("id").replace(".*:(.*)".toRegex(), "$1") == enchantment.key.key
                    && (!nbt.getString("id").contains(":") || nbt.getString("id").replace("(.*):.*".toRegex(), "$1") == enchantment.key.namespace)
        }
        return existingEnchantment != null
    }

    override fun getEnchantments(item: ItemStack?): List<CustomEnchantedData> {
        if (item == null) return emptyList()
        val itemStack = CraftItemStack.asNMSCopy(item)
        val tag = itemStack.getOrCreateTag()
        val storedEnchantments = (tag["storedEnchantments"] ?: NBTTagList()) as NBTTagList
        val enchantments = mutableListOf<CustomEnchantedData>()
        storedEnchantments.forEach { nbt ->
            val t = nbt as NBTTagCompound
            val ench = getById(t.getString("id"))
            if (ench == null) {
                CustomEnchantmentsPlugin.instance.logger.warning("Skipping enchantment ${t.getString("id")}")
                return@forEach
            }
            enchantments.add(CustomEnchantedData(ench, t.getInt("lvl"), t.getBoolean("anti")))
        }
        return enchantments
    }

    override fun hasEnchantmentOfLevel(item: ItemStack, enchantment: CustomEnchantment, level: Int): Boolean {
        val itemStack = CraftItemStack.asNMSCopy(item)
        val tag = itemStack.getOrCreateTag()
        val storedEnchantments = (tag["storedEnchantments"] ?: NBTTagList()) as NBTTagList
        val existingEnchantment = storedEnchantments.find { nbt ->
            (nbt as NBTTagCompound).getString("id").replace(".*:(.*)".toRegex(), "$1") == enchantment.key.key
                    && (!nbt.getString("id").contains(":") || nbt.getString("id").replace("(.*):.*".toRegex(), "$1") == enchantment.key.namespace)
        } ?: return false
        val e = (existingEnchantment as NBTTagCompound)
        return e.getInt("lvl") == level
    }

    override fun getEnchantmentLevel(item: ItemStack?, enchantment: CustomEnchantment): Int {
        if (item == null) return 0
        val itemStack = CraftItemStack.asNMSCopy(item)
        val tag = itemStack.getOrCreateTag()
        val storedEnchantments = (tag["storedEnchantments"] ?: NBTTagList()) as NBTTagList
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

private fun Component.textContent() = if (this is TextComponent) this.content() else this.toString()

private fun Component.noItalic() = decoration(TextDecoration.ITALIC, false)
