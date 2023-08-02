package xyz.acrylicstyle.customenchantments

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.AnvilInventory
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import xyz.acrylicstyle.customenchantments.EnchantmentManagerImpl.Companion.getNearbyBlocks
import xyz.acrylicstyle.customenchantments.api.CustomEnchantments
import xyz.acrylicstyle.customenchantments.api.EnchantmentManager
import xyz.acrylicstyle.customenchantments.api.enchantment.ActivateType
import xyz.acrylicstyle.customenchantments.api.enchantment.CustomEnchantment
import xyz.acrylicstyle.customenchantments.commands.RootCommand
import xyz.acrylicstyle.customenchantments.enchantments.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max

class CustomEnchantmentsPlugin : JavaPlugin(), Listener, CustomEnchantments {
    companion object {
        lateinit var config: CustomEnchantmentConfig
        val instance: CustomEnchantmentsPlugin
            get() = getPlugin(CustomEnchantmentsPlugin::class.java)

        val recipes = mutableListOf<NamespacedKey>()
    }

    private val manager = EnchantmentManagerImpl(this)

    override fun onEnable() {
        CustomEnchantmentsPlugin.config = CustomEnchantmentConfig(config)
        logger.info("Registering enchantments")
        manager.registerEnchantment(JungleAxeEnchant())
        manager.registerEnchantment(SpeedEnchant())
        manager.registerEnchantment(RegenerationEnchant())
        manager.registerEnchantment(AntiHungryEnchant())
        manager.registerEnchantment(JumpEnchant())
        manager.registerEnchantment(GrowthEnchant())
        object: BukkitRunnable() {
            override fun run() {
                this@CustomEnchantmentsPlugin.config.getConfigurationSection("effects")?.getValues(true)?.keys?.forEach { key ->
                    if (key.contains(":")) {
                        logger.warning("Skipping: Do not include ':' in the key: effects.$key")
                        return@forEach
                    }
                    val enchantment = manager.getById(key)
                    if (enchantment == null) {
                        logger.warning("Skipping: Invalid enchantment key: effects.$key")
                        return@forEach
                    }
                    if (recipes.find { namespacedKey -> namespacedKey == enchantment.key } != null) {
                        logger.warning("Skipping: Duplicate enchantment key: effects.$key")
                        return@forEach
                    }
                    val recipe = ShapedRecipe(enchantment.key, enchantment.getEnchantmentBook(1, false))
                    recipe.shape("012", "345", "678")
                    for (i in 0..8) {
                        val item = CustomEnchantmentsPlugin.config.getItem("effects.$key.crafting.slot$i") ?: continue
                        recipe.setIngredient(i.toString()[0], item)
                    }
                    recipes.add(enchantment.key)
                    try {
                        Bukkit.addRecipe(recipe)
                    } catch (e: IllegalStateException) {
                        logger.warning("Ignoring duplicate recipe of effects.$key")
                    }
                }
            }
        }.runTaskLater(this, 1)
        Bukkit.getPluginManager().registerEvents(this, this)
        getCommand("customenchantments")?.setExecutor(RootCommand)
    }

    override fun onDisable() {
        Bukkit.getOnlinePlayers().forEach { player ->
            CustomEnchantment.deactivateAllActiveEffects(player)
        }
        recipes.forEach { key ->
            Bukkit.removeRecipe(key)
        }
    }

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        CustomEnchantment.deactivateAllActiveEffects(e.player)
        e.player.walkSpeed = 0.2F
    }

    @EventHandler
    fun onPlayerQuit(e: PlayerQuitEvent) {
        CustomEnchantment.deactivateAllActiveEffects(e.player)
        e.player.walkSpeed = 0.2F
    }

    @EventHandler
    fun onInventoryClick(e: InventoryClickEvent) {
        if (e.inventory !is AnvilInventory || e.clickedInventory !is AnvilInventory) return
        val anvil = e.inventory as AnvilInventory
        if (e.slotType != InventoryType.SlotType.RESULT) return
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
        if  (anvil.firstItem != null && anvil.firstItem!!.type == Material.ENCHANTED_BOOK && anvil.secondItem?.type == Material.REDSTONE_TORCH) {
            e.isCancelled = true
            (e.whoClicked as Player).playSound(e.whoClicked.location, Sound.BLOCK_ANVIL_USE, 1F, 1F)
            if (anvil.result != null) {
                e.whoClicked
                    .inventory
                    .addItem(anvil.result!!)
                    .forEach { (_, item) -> e.whoClicked.world.dropItem(e.whoClicked.location, item) }
                anvil.firstItem = null
                if (anvil.secondItem!!.amount == 1) {
                    anvil.secondItem = null
                } else {
                    anvil.secondItem!!.amount -= 1
                }
                anvil.result = null
            }
        }
    }

    @EventHandler
    fun onPrepareAnvil(e: PrepareAnvilEvent) {
        val first = e.inventory.firstItem
        val second = e.inventory.secondItem
        val eBook = first != null && first.type == Material.ENCHANTED_BOOK
        if (manager.hasEnchantments(first)) {
            if (manager.hasEnchantments(second)) { // first = true, second = true
                var result = first!!.clone()
                manager.getEnchantments(first).forEach { enchantment ->
                    result = manager.removeEnchantment(result, enchantment.enchantment)
                }
                manager.getEnchantments(second).forEach { enchantment ->
                    result = manager.removeEnchantment(result, enchantment.enchantment)
                }
                manager.getEnchantments(first).forEach { enchantment ->
                    result = manager.applyEnchantment(result, enchantment.enchantment, enchantment.level, enchantment.isAnti)
                }
                manager.getEnchantments(second).forEach { enchantment ->
                    if (!eBook && !enchantment.enchantment.canEnchantItem(result)) return@forEach
                    val level1 = manager.getEnchantmentLevel(first, enchantment.enchantment)
                    val level2 = enchantment.level
                    if (level1 != level2) {
                        if (!enchantment.isAnti) result = manager.applyEnchantment(result, enchantment.enchantment, level1.coerceAtLeast(level2), false)
                        return@forEach
                    }
                    val level = if (level2 >= enchantment.enchantment.getMaximumAnvilableLevel()) level2 else (level2 + 1).coerceAtMost(enchantment.enchantment.maxLevel)
                    //val level = if (level2 >= enchantment.enchantment.getMaximumAnvilableLevel() && !eBook) level2 else (level2 + 1).coerceAtMost(enchantment.enchantment.maxLevel)
                    if (!enchantment.isAnti) result = manager.applyEnchantment(result, enchantment.enchantment, level, false)
                }
                e.inventory.result = result
                e.result = result
            } else { // first = true, second = false
                if (eBook && second != null && second.type == Material.REDSTONE_TORCH) { // anti / de-anti enchant
                    var result = first!!.clone()
                    manager.getEnchantments(first).forEach { enchantment ->
                        result = manager.removeEnchantment(result, enchantment.enchantment)
                    }
                    manager.getEnchantments(first).forEach { enchantment ->
                        result = manager.applyEnchantment(result, enchantment.enchantment, enchantment.level, !enchantment.isAnti)
                    }
                    e.inventory.result = result
                    e.result = result
                }
                return // it must be something that we cannot modify
            }
        } else {
            if (first != null && manager.hasEnchantments(second)) { // first = false, second = true
                var result = first.clone()
                manager.getEnchantments(second).forEach { enchantment ->
                    if (!enchantment.enchantment.canEnchantItem(result)) return@forEach
                    val level = manager.getEnchantmentLevel(second, enchantment.enchantment)
                    result = if (enchantment.isAnti) {
                        manager.removeEnchantment(result, enchantment.enchantment)
                    } else {
                        manager.applyEnchantment(result, enchantment.enchantment, level, false)
                    }
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
        // order is *VERY* important
        manager.getEnchantments(e.oldItem).forEach { enchantment ->
            enchantment.enchantment.deactivate(e.player, manager.getEnchantmentLevel(e.oldItem, enchantment.enchantment))
        }
        manager.getEnchantments(e.newItem).forEach { enchantment ->
            if (enchantment.enchantment.canActivateEnchantment(ActivateType.ARMOR_CHANGED, e.newItem!!))
                enchantment.enchantment.activate(e.player, manager.getEnchantmentLevel(e.newItem, enchantment.enchantment))
        }
    }

    @EventHandler
    fun onBlockBreak(e: BlockBreakEvent) {
        val item = e.player.inventory.itemInMainHand
        val type = e.block.type
        if (e.expToDrop != -1
            && item.type.name.endsWith("AXE")
            && manager.hasEnchantment(item, manager.getEnchantment(JungleAxeEnchant::class.java)!!)
            && type.name.endsWith("_LOG")) {
            val blocks = 10 * manager.getEnchantmentLevel(item, manager.getEnchantment(JungleAxeEnchant::class.java)!!)
            val harvestedBlocks = AtomicInteger(1)
            val period = max(10 - item.getEnchantmentLevel(Enchantment.DIG_SPEED), 1).toLong()
            val loc: AtomicReference<Location?> = AtomicReference()
            val checkedLocation = mutableSetOf<Location>()
            val locationsToCheck = mutableSetOf(e.block.location)
            val checkedBlock = mutableSetOf<Location>()
            object: BukkitRunnable() {
                override fun run() {
                    if (harvestedBlocks.get() > blocks) {
                        this.cancel()
                        return
                    }
                    val theBlock: Block
                    while (true) {
                        if (loc.get() == null) {
                            loc.set(locationsToCheck.firstOrNull())
                            if (loc.get() == null) {
                                this.cancel()
                                return
                            }
                            checkedLocation.add(locationsToCheck.first())
                            locationsToCheck.remove(locationsToCheck.first())
                        }
                        val nearbyBlock = loc.get()!!.getNearbyBlocks(1)
                            .filter { block -> !checkedBlock.contains(block.location) }
                            .filter { block -> !block.type.isAir }
                            .firstOrNull { block -> block.type == type }
                        if (nearbyBlock == null) {
                            loc.set(null)
                            continue
                        }
                        theBlock = nearbyBlock
                        break
                    }
                    if (BlockBreakEvent(theBlock, e.player).apply { expToDrop = -1 }.callEvent()) {
                        theBlock.breakNaturally()
                        theBlock.world.playSound(theBlock.location, Sound.BLOCK_WOOD_BREAK, 1F, 1F)
                        if (!checkedLocation.contains(theBlock.location)) locationsToCheck.add(theBlock.location)
                        harvestedBlocks.incrementAndGet()
                        checkedBlock.add(theBlock.location)
                    }
                }
            }.runTaskTimer(this, period, period)
        }
    }

    @EventHandler
    fun onPlayerItemHeld(e: PlayerItemHeldEvent) {
        val oldItem = e.player.inventory.getItem(e.previousSlot)
        manager.getEnchantments(oldItem).forEach { enchantment ->
            enchantment.enchantment.deactivate(e.player, manager.getEnchantmentLevel(oldItem, enchantment.enchantment))
        }
        val newItem = e.player.inventory.getItem(e.newSlot)
        manager.getEnchantments(newItem).forEach { enchantment ->
            if (enchantment.enchantment.canActivateEnchantment(ActivateType.ITEM_HELD, newItem!!))
                enchantment.enchantment.activate(e.player, manager.getEnchantmentLevel(newItem, enchantment.enchantment))
        }
    }

    override fun getManager(): EnchantmentManager = manager
}
