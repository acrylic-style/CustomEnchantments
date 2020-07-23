package xyz.acrylicstyle.customEnchantments

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.enchantments.EnchantmentTarget
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.inventory.AnvilInventory
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import util.CollectionList
import xyz.acrylicstyle.customEnchantments.api.CustomEnchantments
import xyz.acrylicstyle.customEnchantments.api.EnchantmentManager
import xyz.acrylicstyle.customEnchantments.api.enchantment.CustomEnchantment
import xyz.acrylicstyle.customEnchantments.commands.CETabCompleter
import xyz.acrylicstyle.customEnchantments.enchantments.*
import xyz.acrylicstyle.tomeito_api.TomeitoAPI
import xyz.acrylicstyle.tomeito_api.utils.Log

class CustomEnchantmentsPlugin : JavaPlugin(), Listener, CustomEnchantments {
    companion object {
        lateinit var config: CustomEnchantmentConfig
        lateinit var instance: CustomEnchantmentsPlugin
        val recipes = CollectionList<NamespacedKey>()
    }

    private val manager = EnchantmentManagerImpl(this)

    override fun onLoad() {
        instance = this
    }

    override fun onEnable() {
        CustomEnchantmentsPlugin.config = CustomEnchantmentConfig()
        Log.info("Registering enchantments")
        manager.registerEnchantment(SpeedEnchant())
        manager.registerEnchantment(RegenerationEnchant())
        manager.registerEnchantment(AntiHungryEnchant())
        manager.registerEnchantment(JumpEnchant())
        manager.registerEnchantment(GrowthEnchant())
        object: BukkitRunnable() {
            override fun run() {
                CustomEnchantmentsPlugin.config.getConfigSectionValue("effects", false).keys.forEach { key ->
                    if (key.contains(":")) {
                        Log.warn("Do not include ':' in the key: effects.$key")
                        return@forEach
                    }
                    val enchantment = manager.getById(key)
                    if (enchantment == null) {
                        Log.warn("Skipping invalid enchantment key: effects.$key")
                        return@forEach
                    }
                    if (recipes.find { namespacedKey -> namespacedKey == enchantment.key } != null) {
                        Log.warn("Skipping duplicate enchantment key: effects.$key")
                        return@forEach
                    }
                    val recipe = ShapedRecipe(enchantment.key, enchantment.getEnchantmentBook(1))
                    recipe.shape("012", "345", "678")
                    for (i in 0..8) {
                        val item = CustomEnchantmentsPlugin.config.getItem("effects.$key.crafting.slot$i") ?: continue
                        recipe.setIngredient(i.toString()[0], item)
                    }
                    recipes.add(enchantment.key)
                    Bukkit.addRecipe(recipe)
                }
            }
        }.runTaskLater(this, 1)
        Bukkit.getPluginManager().registerEvents(this, this)
        TomeitoAPI.registerTabCompleter("customenchantments", CETabCompleter())
        TomeitoAPI.getInstance().registerCommands(this.classLoader, "customenchantments", "xyz.acrylicstyle.customEnchantments.commands")
    }

    override fun onDisable() {
        recipes.forEach { key ->
            Bukkit.removeRecipe(key)
        }
    }

    @Suppress("SENSELESS_COMPARISON") // nice wrong detection
    @EventHandler
    fun onInventoryClick(e: InventoryClickEvent) {
        if (e.inventory !is AnvilInventory) return
        val anvil = e.inventory as AnvilInventory
        if (manager.hasEnchantments(anvil.secondItem)) {
            anvil.result?.let {
                e.isCancelled = true
                (e.whoClicked as Player).playSound(e.whoClicked.location, Sound.BLOCK_ANVIL_USE, 1F, 1F)
                if (anvil.result != null) {
                    e.whoClicked
                            .inventory
                            .addItem(it)
                            .forEach { (_, item) -> e.whoClicked.world.dropItem(e.whoClicked.location, item) }
                    anvil.firstItem = null
                    anvil.secondItem = null
                    anvil.result = null
                }
            }
        }
    }

    @EventHandler
    fun onPrepareAnvil(e: PrepareAnvilEvent) {
        val first = e.inventory.firstItem
        val second = e.inventory.secondItem
        if (manager.hasEnchantments(first)) {
            if (manager.hasEnchantments(second)) { // first = true, second = true
                var result = first!!.clone()
                manager.getEnchantments(second).forEach { enchantment ->
                    val level1 = manager.getEnchantmentLevel(first, enchantment)
                    val level2 = manager.getEnchantmentLevel(second, enchantment)
                    if (level1 != level2 && level1 > level2) return@forEach
                    result = manager.removeEnchantment(result, enchantment)
                    result = manager.applyEnchantment(result, enchantment, (level2 + 1).coerceAtMost(enchantment.maxLevel))
                }
                e.inventory.result = result
                e.result = result
            } else { // first = true, second = false
                return // it must be something that we cannot modify
            }
        } else {
            if (first != null && manager.hasEnchantments(second)) { // first = false, second = true
                var result = first.clone()
                manager.getEnchantments(second).forEach { enchantment ->
                    val level = manager.getEnchantmentLevel(second, enchantment)
                    result = manager.applyEnchantment(result, enchantment, level)
                }
                e.inventory.result = result
                e.result = result
            } else { // first = false, second = false
                return
            }
        }
    }

    @EventHandler
    fun onPlayerDeath(e: PlayerDeathEvent) {
        CustomEnchantment.deactivateAllActiveEffects(e.entity)
    }

    @EventHandler
    fun onPlayerArmorChange(e: PlayerArmorChangeEvent) {
        manager.getEnchantments(e.newItem).forEach { enchantment ->
            if (enchantment.itemTarget.name.startsWith("ARMOR") || enchantment.itemTarget == EnchantmentTarget.WEARABLE)
                enchantment.activate(e.player, manager.getEnchantmentLevel(e.newItem, enchantment))
        }
        manager.getEnchantments(e.oldItem).forEach { enchantment ->
            if (enchantment.itemTarget.name.startsWith("ARMOR") || enchantment.itemTarget == EnchantmentTarget.WEARABLE)
                enchantment.deactivate(e.player, manager.getEnchantmentLevel(e.oldItem, enchantment))
        }
    }

    @EventHandler
    fun onPlayerItemHeld(e: PlayerItemHeldEvent) {
        val newItem = e.player.inventory.getItem(e.newSlot)
        manager.getEnchantments(newItem).forEach { enchantment ->
            if (!enchantment.itemTarget.name.startsWith("ARMOR") && enchantment.itemTarget != EnchantmentTarget.WEARABLE)
                enchantment.activate(e.player, manager.getEnchantmentLevel(newItem, enchantment))
        }
        val oldItem = e.player.inventory.getItem(e.previousSlot)
        manager.getEnchantments(oldItem).forEach { enchantment ->
            if (!enchantment.itemTarget.name.startsWith("ARMOR") && enchantment.itemTarget != EnchantmentTarget.WEARABLE)
                enchantment.deactivate(e.player, manager.getEnchantmentLevel(oldItem, enchantment))
        }
    }

    override fun getManager(): EnchantmentManager = manager
}