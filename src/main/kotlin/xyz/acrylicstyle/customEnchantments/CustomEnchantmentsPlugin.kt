package xyz.acrylicstyle.customEnchantments

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent
import org.bukkit.Bukkit
import org.bukkit.enchantments.EnchantmentTarget
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.plugin.java.JavaPlugin
import xyz.acrylicstyle.customEnchantments.api.CustomEnchantments
import xyz.acrylicstyle.customEnchantments.api.EnchantmentManager
import xyz.acrylicstyle.customEnchantments.api.enchantment.CustomEnchantment
import xyz.acrylicstyle.customEnchantments.commands.CETabCompleter
import xyz.acrylicstyle.customEnchantments.enchantments.AntiHungryEnchant
import xyz.acrylicstyle.customEnchantments.enchantments.GrowthEnchant
import xyz.acrylicstyle.customEnchantments.enchantments.JumpEnchant
import xyz.acrylicstyle.tomeito_api.TomeitoAPI

class CustomEnchantmentsPlugin : JavaPlugin(), Listener, CustomEnchantments {
    companion object {
        // val log = Logger.getLogger("CustomEnchantments")

        lateinit var instance: CustomEnchantmentsPlugin
    }

    private val manager = EnchantmentManagerImpl(this)

    override fun onLoad() {
        instance = this
    }

    override fun onEnable() {
        manager.registerEnchantment(AntiHungryEnchant())
        manager.registerEnchantment(JumpEnchant())
        manager.registerEnchantment(GrowthEnchant())
        Bukkit.getPluginManager().registerEvents(this, this)
        TomeitoAPI.registerTabCompleter("customenchantments", CETabCompleter())
        TomeitoAPI.getInstance().registerCommands(this.classLoader, "customenchantments", "xyz.acrylicstyle.customEnchantments.commands")
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