package me.kevinnovak.infiniteinventory;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class InfiniteInventory extends JavaPlugin implements Listener{
    // ======================
    // Enable
    // ======================
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        if (getConfig().getBoolean("metrics")) {
            try {
                MetricsLite metrics = new MetricsLite(this);
                metrics.start();
                Bukkit.getServer().getLogger().info("[InfiniteInventory] Metrics Enabled!");
            } catch (IOException e) {
                Bukkit.getServer().getLogger().info("[InfiniteInventory] Failed to Start Metrics.");
            }
        } else {
            Bukkit.getServer().getLogger().info("[InfiniteInventory] Metrics Disabled.");
        }
        Bukkit.getServer().getLogger().info("[InfiniteInventory] Plugin Enabled!");
    }
    
    // ======================
    // Disable
    // ======================
    public void onDisable() {
        Bukkit.getServer().getLogger().info("[InfiniteInventory] Plugin Disabled!");
    }
}