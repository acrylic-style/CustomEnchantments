package xyz.acrylicstyle.customEnchantments

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.enchantments.Enchantment
import org.bukkit.enchantments.EnchantmentTarget
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.AnvilInventory
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import util.CollectionList
import util.CollectionSet
import util.MathUtils
import xyz.acrylicstyle.customEnchantments.EnchantmentManagerImpl.Companion.getNearbyBlocks
import xyz.acrylicstyle.customEnchantments.api.CustomEnchantments
import xyz.acrylicstyle.customEnchantments.api.EnchantmentManager
import xyz.acrylicstyle.customEnchantments.api.enchantment.CustomEnchantment
import xyz.acrylicstyle.customEnchantments.commands.CETabCompleter
import xyz.acrylicstyle.customEnchantments.enchantments.*
import xyz.acrylicstyle.tomeito_api.TomeitoAPI
import xyz.acrylicstyle.tomeito_api.utils.Log
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

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
        manager.registerEnchantment(JungleAxeEnchant())
        manager.registerEnchantment(MukiEnchant())
        manager.registerEnchantment(RenyokoEnchant())
        manager.registerEnchantment(MeEnchant())
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
                    val recipe = ShapedRecipe(enchantment.key, enchantment.getEnchantmentBook(1, false))
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
    }

    @EventHandler
    fun onPlayerQuit(e: PlayerQuitEvent) {
        CustomEnchantment.deactivateAllActiveEffects(e.player)
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
                    .addItem(anvil.result)
                    .forEach { (_, item) -> e.whoClicked.world.dropItem(e.whoClicked.location, item) }
                anvil.firstItem = null
                if (anvil.secondItem!!.amount == 1) {
                    anvil.secondItem = null
                } else {
                    anvil.secondItem!!.amount = anvil.secondItem!!.amount - 1
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
            if (enchantment.enchantment.itemTarget.name.startsWith("ARMOR") || enchantment.enchantment.itemTarget == EnchantmentTarget.WEARABLE)
                enchantment.enchantment.deactivate(e.player, manager.getEnchantmentLevel(e.oldItem, enchantment.enchantment))
        }
        manager.getEnchantments(e.newItem).forEach { enchantment ->
            if (enchantment.enchantment.itemTarget.name.startsWith("ARMOR") || enchantment.enchantment.itemTarget == EnchantmentTarget.WEARABLE)
                enchantment.enchantment.activate(e.player, manager.getEnchantmentLevel(e.newItem, enchantment.enchantment))
        }
    }

    private val victim1 = UUID.fromString("7ffad749-e54d-4a13-908d-ed8807eb6d25")
    private val victim2 = UUID.fromString("9c29137b-54a8-4e9a-ab22-f614cd23cc3b")
    private val victim3 = UUID.fromString("1865ab8c-700b-478b-9b52-a8c58739df1a")

    @EventHandler
    fun onEntityDamageByEntity(e: EntityDamageByEntityEvent) {
        if (e.entity is Player && e.damager is Player) {
            val player = e.entity as Player
            val damager = e.damager as Player
            val item = damager.inventory.itemInMainHand
            if (player.uniqueId == victim1) {
                if (manager.hasEnchantment(item, manager.getEnchantment(MukiEnchant::class.java)!!)) {
                    e.damage = e.damage * (10 * manager.getEnchantmentLevel(item, manager.getEnchantment(MukiEnchant::class.java)!!))
                }
            }
            if (player.uniqueId == victim2) {
                if (manager.hasEnchantment(item, manager.getEnchantment(RenyokoEnchant::class.java)!!)) {
                    e.damage = e.damage * (10 * manager.getEnchantmentLevel(item, manager.getEnchantment(RenyokoEnchant::class.java)!!))
                }
            }
            if (player.uniqueId == victim3) {
                if (manager.hasEnchantment(damager.inventory.itemInMainHand, manager.getEnchantment(MeEnchant::class.java)!!)) {
                    e.damage = e.damage * (1000 * manager.getEnchantmentLevel(item, manager.getEnchantment(MeEnchant::class.java)!!))
                }
            }
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
            val period = MathUtils.max(10 - item.getEnchantmentLevel(Enchantment.DIG_SPEED), 1).toLong()
            val loc: AtomicReference<Location?> = AtomicReference()
            val checkedLocation = CollectionSet<Location>()
            val locationsToCheck = CollectionSet<Location>(e.block.location)
            val checkedBlock = CollectionSet<Location>()
            object: BukkitRunnable() {
                override fun run() {
                    if (harvestedBlocks.get() > blocks) {
                        this.cancel()
                        return
                    }
                    val theBlock: Block
                    while (true) {
                        if (loc.get() == null) {
                            loc.set(locationsToCheck.first())
                            if (loc.get() == null) {
                                this.cancel()
                                return
                            }
                            checkedLocation.add(locationsToCheck.first())
                            locationsToCheck.remove(locationsToCheck.first())
                        }
                        val nearbyBlock = loc.get()!!.getNearbyBlocks(1)
                            .nonNull()
                            .filter { block -> !checkedBlock.contains(block.location) }
                            .filter { block -> !block.type.isAir }
                            .filter { block -> block.type == type }
                            .first()
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
    fun onPlayerInteract(e: PlayerInteractEvent) {
        if (e.action == Action.RIGHT_CLICK_AIR || e.action == Action.RIGHT_CLICK_BLOCK) {
            if (e.player.uniqueId == victim3) {
                if (manager.hasEnchantment(e.player.inventory.itemInMainHand, manager.getEnchantment(MeEnchant::class.java)!!)) {
                    e.player.health = 0.0
                }
            }
        }
    }

    @EventHandler
    fun onPlayerItemHeld(e: PlayerItemHeldEvent) {
        val oldItem = e.player.inventory.getItem(e.previousSlot)
        manager.getEnchantments(oldItem).forEach { enchantment ->
            if (!enchantment.enchantment.itemTarget.name.startsWith("ARMOR") && enchantment.enchantment.itemTarget != EnchantmentTarget.WEARABLE)
                enchantment.enchantment.deactivate(e.player, manager.getEnchantmentLevel(oldItem, enchantment.enchantment))
        }
        val newItem = e.player.inventory.getItem(e.newSlot)
        manager.getEnchantments(newItem).forEach { enchantment ->
            if (!enchantment.enchantment.itemTarget.name.startsWith("ARMOR") && enchantment.enchantment.itemTarget != EnchantmentTarget.WEARABLE)
                enchantment.enchantment.activate(e.player, manager.getEnchantmentLevel(newItem, enchantment.enchantment))
        }
    }

    override fun getManager(): EnchantmentManager = manager
}
