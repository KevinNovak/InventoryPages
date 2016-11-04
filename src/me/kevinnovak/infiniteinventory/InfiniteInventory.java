package me.kevinnovak.infiniteinventory;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
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
    
    // =========================
    // Login
    // =========================
    CustomInventory test;
    @EventHandler
    public void playerJoin(PlayerJoinEvent event) throws InterruptedException {
    	test = new CustomInventory(event.getPlayer());
    }
    
//    // =========================
//    // Open Inventory
//    // =========================
//    @EventHandler
//    public void onInventoryOpen(InventoryOpenEvent event){
//    	HumanEntity human = event.getPlayer();
//    	if (human instanceof Player) {
//    		Player player = (Player) human;
//    		myInventory.setItem(35, nextItem);
//    		player.closeInventory();
//    		event.setCancelled(true);
//    		player.openInventory(myInventory);
//    	}
//    }
    
//    // =========================
//    // Click Next
//    // =========================
//    @EventHandler
//    public void onInventoryClick(InventoryClickEvent event) {
//      HumanEntity human = event.getWhoClicked();
//      if (human instanceof Player) {
//    	  Player player = (Player) human;
//          if (event.getInventory().getTitle().equals("Inv Shop")) {
//              event.setCancelled(true);
//              player.updateInventory();
//                 
//          }
//      }
//      return;
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
        @SuppressWarnings("unused")
		Player player = (Player) sender;
        // ======================
        // Player
        // ======================
        if (cmd.getName().equalsIgnoreCase("testsave")) {
        	test.savePage();
        }
        if (cmd.getName().equalsIgnoreCase("testnext")) {
        	test.nextPage();
        }
        if (cmd.getName().equalsIgnoreCase("testprev")) {
        	test.prevPage();
        }
		return true;
    }
}