package me.kevinnovak.infiniteinventory;
import java.io.IOException;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class InfiniteInventory extends JavaPlugin implements Listener{
	private HashMap<String, CustomInventory> playerInvs = new HashMap<String, CustomInventory>();
	
	
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
        
        loadInvs();
    }

	// ======================
    // Disable
    // ======================
    public void onDisable() {
    	saveInvs();
        Bukkit.getServer().getLogger().info("[InfiniteInventory] Plugin Disabled!");
    }
    
    private void loadInvs() {
		// TODO Auto-generated method stub
		
	}
    
    private void saveInvs() {
		// TODO Auto-generated method stub
		
	}

	// =========================
    // Login
    // =========================
    @EventHandler
    public void playerJoin(PlayerJoinEvent event) throws InterruptedException {
    	Player player = event.getPlayer();
    	if (!playerInvs.containsKey(player.getName())) {
    		CustomInventory playerInv = new CustomInventory(player);
    		playerInvs.put(player.getName(), playerInv);
    	} else {
    		playerInvs.get(player.getName()).setPlayer(player);
    		playerInvs.get(player.getName()).showPage(0);
    	}
    }
    
    // =========================
    // Logout
    // =========================
    @EventHandler
    public void playerQuit(PlayerQuitEvent event) throws InterruptedException {
    	Player player = event.getPlayer();
    	if (!playerInvs.containsKey(player.getName())) {
    		playerInvs.get(player.getName()).saveCurrentPage();
    	}
    }
    
//    // =========================
//    // Open Inventory
//    // =========================
//    @EventHandler
//    public void onInventoryOpen(InventoryOpenEvent event){
//    }
    
//    // =========================
//    // Inventory Click
//    // =========================
//    @EventHandler
//    public void onInventoryClick(InventoryClickEvent event) {
//    }
    
    // ======================
    // Commands
    // ======================
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        // ======================
        // Console
        // ======================
        if (!(sender instanceof Player)) {
            return true;
        }
		Player player = (Player) sender;
        // ======================
        // Player
        // ======================
        if (cmd.getName().equalsIgnoreCase("testsave")) {
        	playerInvs.get(player.getName()).saveCurrentPage();
        }
        if (cmd.getName().equalsIgnoreCase("testnext")) {
        	playerInvs.get(player.getName()).nextPage();
        }
        if (cmd.getName().equalsIgnoreCase("testprev")) {
        	playerInvs.get(player.getName()).prevPage();
        }
		return true;
    }
}