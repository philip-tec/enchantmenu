package me.enchant;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class EnchantPlugin extends JavaPlugin implements Listener {

    private static EnchantPlugin instance;
    private PressurePlateLauncher pressurePlateLauncher;

    @Override
    public void onEnable() {
        instance = this;

        
        saveDefaultConfig();

        
        EnchantMenu enchantMenu = new EnchantMenu();
        getServer().getPluginManager().registerEvents(enchantMenu, this);
        getCommand("enchantreload").setExecutor(enchantMenu);

        
        pressurePlateLauncher = new PressurePlateLauncher();
        getServer().getPluginManager().registerEvents(pressurePlateLauncher, this);
        
        
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("EnchantPlugin enabled with Pressure Plate Launcher!");
    }

    @Override
    public void onDisable() {
        getLogger().info("EnchantPlugin disabled.");
    }

    public static EnchantPlugin getInstance() {
        return instance;
    }
    
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (pressurePlateLauncher != null) {
            pressurePlateLauncher.cleanupPlayer(event.getPlayer().getUniqueId());
        }
    }
}