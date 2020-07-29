package xyz.acrylicstyle.customEnchantments

import com.codingforcookies.armorequip.ArmorEquipEvent
import com.codingforcookies.armorequip.ArmorListener
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.EnchantmentTarget
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.AnvilInventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import util.CollectionList
import util.ICollectionList
import xyz.acrylicstyle.customEnchantments.api.CustomEnchantments
import xyz.acrylicstyle.customEnchantments.api.EnchantmentManager
import xyz.acrylicstyle.customEnchantments.api.enchantment.CustomEnchantment
import xyz.acrylicstyle.customEnchantments.commands.CETabCompleter
import xyz.acrylicstyle.customEnchantments.enchantments.*
import xyz.acrylicstyle.tomeito_api.TomeitoAPI
import xyz.acrylicstyle.tomeito_api.utils.Log

class CustomEnchantmentsPlugin : JavaPlugin(), Listener, CustomEnchantments {
    private val manager = EnchantmentManagerImpl(this)

    override fun onLoad() {
        instance = this
    }

    override fun onEnable() {
        CustomEnchantmentsPlugin.config = CustomEnchantmentConfig()
        Bukkit.getPluginManager().registerEvents(ArmorListener(blocked), this)
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
                    if (recipes.find { namespacedKey -> namespacedKey == enchantment.id } != null) {
                        Log.warn("Skipping duplicate enchantment key: effects.$key")
                        return@forEach
                    }
                    val recipe = ShapedRecipe(enchantment.getEnchantmentBook(1))
                    recipe.shape("012", "345", "678")
                    for (i in 0..8) {
                        val item = CustomEnchantmentsPlugin.config.getItem("effects.$key.crafting.slot$i") ?: continue
                        val f = ShapedRecipe::class.java.getDeclaredField("ingredients")
                        f.isAccessible = true
                        @Suppress("UNCHECKED_CAST") val map = f.get(recipe) as MutableMap<Char, ItemStack>
                        map[i.toString()[0]] = item
                    }
                    recipes.add(enchantment.id)
                    Bukkit.addRecipe(recipe)
                }
            }
        }.runTaskLater(this, 1)
        object: BukkitRunnable() {
            override fun run() {
                Bukkit.getOnlinePlayers().forEach { p ->
                    if (p.openInventory.topInventory !is AnvilInventory) return@forEach
                    val first = p.openInventory.topInventory.getItem(0)
                    val second = p.openInventory.topInventory.getItem(1)
                    val eBook = first != null && first.type == Material.ENCHANTED_BOOK
                    if (manager.hasEnchantments(first)) {
                        if (manager.hasEnchantments(second)) { // first = true, second = true
                            var result = first!!.clone()
                            manager.getEnchantments(second).forEach f@ { enchantment ->
                                if (!eBook && !enchantment.canEnchantItem(result)) return@f
                                val level1 = manager.getEnchantmentLevel(first, enchantment)
                                val level2 = manager.getEnchantmentLevel(second, enchantment)
                                if (level1 != level2) {
                                    result = manager.removeEnchantment(result, enchantment)
                                    result = manager.applyEnchantment(result, enchantment, level1.coerceAtLeast(level2))
                                    return@f
                                }
                                result = manager.removeEnchantment(result, enchantment)
                                val level = if (level2 >= enchantment.getMaximumAnvilAbleLevel() && !eBook) level2 else (level2 + 1).coerceAtMost(enchantment.maxLevel)
                                result = manager.applyEnchantment(result, enchantment, level)
                            }
                            p.openInventory.topInventory.setItem(2, result)
                        } else { // first = true, second = false
                            return // it must be something that we cannot modify
                        }
                    } else {
                        if (first != null && manager.hasEnchantments(second)) { // first = false, second = true
                            var result = first.clone()
                            manager.getEnchantments(second).forEach f@ { enchantment ->
                                if (!enchantment.canEnchantItem(result)) return@f
                                val level = manager.getEnchantmentLevel(second, enchantment)
                                result = manager.applyEnchantment(result, enchantment, level)
                            }
                            p.openInventory.topInventory.setItem(2, result)
                        } else { // first = false, second = false
                            return
                        }
                    }
                }
            }
        }.runTaskTimer(this, 20L, 20L)
        Bukkit.getPluginManager().registerEvents(this, this)
        TomeitoAPI.registerTabCompleter("customenchantments", CETabCompleter())
        TomeitoAPI.getInstance().registerCommands(this.classLoader, "customenchantments", "xyz.acrylicstyle.customEnchantments.commands")
    }

    override fun onDisable() {
        Bukkit.getOnlinePlayers().forEach { player ->
            CustomEnchantment.deactivateAllActiveEffects(player)
        }
        Bukkit.resetRecipes()
    }

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        e.player.walkSpeed = 0.2F
        ICollectionList.asList(ArrayList(e.player.activePotionEffects)).clone().forEach { pe -> e.player.removePotionEffect(pe.type) }
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
        if (manager.hasEnchantments(anvil.getItem(1))) {
            anvil.getItem(2)?.let {
                e.isCancelled = true
                (e.whoClicked as Player).playSound(e.whoClicked.location, Sound.ANVIL_USE, 1F, 1F)
                e.whoClicked
                        .inventory
                        .addItem(it)
                        .forEach { (_, item) -> e.whoClicked.world.dropItem(e.whoClicked.location, item) }
                anvil.setItem(0, null)
                anvil.setItem(1, null)
                anvil.setItem(2, null)
            }
        }
    }

    @EventHandler
    fun onPlayerDeath(e: PlayerDeathEvent) {
        CustomEnchantment.deactivateAllActiveEffects(e.entity)
    }

    @EventHandler
    fun onArmorEquip(e: ArmorEquipEvent) {
        manager.getEnchantments(e.newArmorPiece).forEach { enchantment ->
            if (enchantment.itemTarget.name.startsWith("ARMOR") || enchantment.itemTarget == EnchantmentTarget.ARMOR)
                enchantment.activate(e.player, manager.getEnchantmentLevel(e.newArmorPiece, enchantment))
        }
        manager.getEnchantments(e.oldArmorPiece).forEach { enchantment ->
            if (enchantment.itemTarget.name.startsWith("ARMOR") || enchantment.itemTarget == EnchantmentTarget.ARMOR)
                enchantment.deactivate(e.player, manager.getEnchantmentLevel(e.oldArmorPiece, enchantment))
        }
    }

    /*
    @EventHandler
    fun onEntityDamageByEntity(e: EntityDamageByEntityEvent) {
        if (e.entity is Player && e.damager is Player) {
            val player = e.entity as Player
            val damager = e.damager as Player
        }
    }
     */

    @EventHandler
    fun onPlayerItemHeld(e: PlayerItemHeldEvent) {
        val newItem = e.player.inventory.getItem(e.newSlot)
        manager.getEnchantments(newItem).forEach { enchantment ->
            if (!enchantment.itemTarget.name.startsWith("ARMOR") && enchantment.itemTarget != EnchantmentTarget.ARMOR)
                enchantment.activate(e.player, manager.getEnchantmentLevel(newItem, enchantment))
        }
        val oldItem = e.player.inventory.getItem(e.previousSlot)
        manager.getEnchantments(oldItem).forEach { enchantment ->
            if (!enchantment.itemTarget.name.startsWith("ARMOR") && enchantment.itemTarget != EnchantmentTarget.ARMOR)
                enchantment.deactivate(e.player, manager.getEnchantmentLevel(oldItem, enchantment))
        }
    }

    override fun getManager(): EnchantmentManager = manager

    companion object {
        lateinit var config: CustomEnchantmentConfig
        lateinit var instance: CustomEnchantmentsPlugin
        val recipes = CollectionList<String>()
        private val blocked = CollectionList<String>()

        init {
            blocked.add("FURNACE")
            blocked.add("CHEST")
            blocked.add("TRAPPED_CHEST")
            blocked.add("BEACON")
            blocked.add("DISPENSER")
            blocked.add("DROPPER")
            blocked.add("HOPPER")
            blocked.add("WORKBENCH")
            blocked.add("ENCHANTMENT_TABLE")
            blocked.add("ENDER_CHEST")
            blocked.add("ANVIL")
            blocked.add("BED_BLOCK")
            blocked.add("FENCE_GATE")
            blocked.add("SPRUCE_FENCE_GATE")
            blocked.add("BIRCH_FENCE_GATE")
            blocked.add("ACACIA_FENCE_GATE")
            blocked.add("JUNGLE_FENCE_GATE")
            blocked.add("DARK_OAK_FENCE_GATE")
            blocked.add("IRON_DOOR_BLOCK")
            blocked.add("WOODEN_DOOR")
            blocked.add("SPRUCE_DOOR")
            blocked.add("BIRCH_DOOR")
            blocked.add("JUNGLE_DOOR")
            blocked.add("ACACIA_DOOR")
            blocked.add("DARK_OAK_DOOR")
            blocked.add("WOOD_BUTTON")
            blocked.add("STONE_BUTTON")
            blocked.add("TRAP_DOOR")
            blocked.add("IRON_TRAPDOOR")
            blocked.add("DIODE_BLOCK_OFF")
            blocked.add("DIODE_BLOCK_ON")
            blocked.add("REDSTONE_COMPARATOR_OFF")
            blocked.add("REDSTONE_COMPARATOR_ON")
            blocked.add("FENCE")
            blocked.add("SPRUCE_FENCE")
            blocked.add("BIRCH_FENCE")
            blocked.add("JUNGLE_FENCE")
            blocked.add("DARK_OAK_FENCE")
            blocked.add("ACACIA_FENCE")
            blocked.add("NETHER_FENCE")
            blocked.add("BREWING_STAND")
            blocked.add("CAULDRON")
            blocked.add("LEGACY_SIGN_POST")
            blocked.add("LEGACY_WALL_SIGN")
            blocked.add("LEGACY_SIGN")
            blocked.add("ACACIA_SIGN")
            blocked.add("ACACIA_WALL_SIGN")
            blocked.add("BIRCH_SIGN")
            blocked.add("BIRCH_WALL_SIGN")
            blocked.add("DARK_OAK_SIGN")
            blocked.add("DARK_OAK_WALL_SIGN")
            blocked.add("JUNGLE_SIGN")
            blocked.add("JUNGLE_WALL_SIGN")
            blocked.add("OAK_SIGN")
            blocked.add("OAK_WALL_SIGN")
            blocked.add("SPRUCE_SIGN")
            blocked.add("SPRUCE_WALL_SIGN")
            blocked.add("LEVER")
            blocked.add("BLACK_SHULKER_BOX")
            blocked.add("BLUE_SHULKER_BOX")
            blocked.add("BROWN_SHULKER_BOX")
            blocked.add("CYAN_SHULKER_BOX")
            blocked.add("GRAY_SHULKER_BOX")
            blocked.add("GREEN_SHULKER_BOX")
            blocked.add("LIGHT_BLUE_SHULKER_BOX")
            blocked.add("LIME_SHULKER_BOX")
            blocked.add("MAGENTA_SHULKER_BOX")
            blocked.add("ORANGE_SHULKER_BOX")
            blocked.add("PINK_SHULKER_BOX")
            blocked.add("PURPLE_SHULKER_BOX")
            blocked.add("RED_SHULKER_BOX")
            blocked.add("SILVER_SHULKER_BOX")
            blocked.add("WHITE_SHULKER_BOX")
            blocked.add("YELLOW_SHULKER_BOX")
            blocked.add("DAYLIGHT_DETECTOR_INVERTED")
            blocked.add("DAYLIGHT_DETECTOR")
            blocked.add("BARREL")
            blocked.add("BLAST_FURNACE")
            blocked.add("SMOKER")
            blocked.add("CARTOGRAPHY_TABLE")
            blocked.add("COMPOSTER")
            blocked.add("GRINDSTONE")
            blocked.add("LECTERN")
            blocked.add("LOOM")
            blocked.add("STONECUTTER")
            blocked.add("BELL")
        }
    }
}