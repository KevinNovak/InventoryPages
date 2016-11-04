package me.kevinnovak.infiniteinventory;
import java.io.IOException;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
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
    }
    
    // ======================
    // Disable
    // ======================
    public void onDisable() {
        Bukkit.getServer().getLogger().info("[InfiniteInventory] Plugin Disabled!");
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
    		player.sendMessage("DNE");
    	} else {
    		playerInvs.get(player.getName()).setPlayer(player);
    		playerInvs.get(player.getName()).showPage(0);
    		player.sendMessage(player.getName());
    		player.sendMessage("E");
    	}
    }
    
    // =========================
    // Logout
    // =========================
    @EventHandler
    public void playerQuit(PlayerQuitEvent event) throws InterruptedException {
    	Player player = event.getPlayer();
    	playerInvs.get(player.getName()).savePage();
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
    
    void addToInventory(Player player) {
    	//List<ItemStack> playerItems;
    	
    	
    	ItemStack[] playerInv = player.getInventory().getContents();
    	ItemStack[] savedInv = new ItemStack[27];
    	for (int i = 0; i<27; i++) {
    		savedInv[i] = playerInv[i+9];
    	}
    	
    	ItemStack[] customInv = new ItemStack[27];
    	ItemStack nextItem = new ItemStack(Material.DIAMOND, 1);
    	ItemStack prevItem = new ItemStack(Material.EMERALD, 1);
    	for (int i = 0; i < 27; i++) {
    		customInv[i] = savedInv[i];
    	}
    	customInv[26] = nextItem;
    	customInv[18] = prevItem;
    	for(int i =0; i<customInv.length; i++) {
    		player.getInventory().setItem(i+9, customInv[i]);
    	}
    }
    
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
        	playerInvs.get(player.getName()).savePage();
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