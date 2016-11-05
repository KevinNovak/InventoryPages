package me.kevinnovak.inventorypages;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class InventoryPages extends JavaPlugin implements Listener{
	private HashMap<String, CustomInventory> playerInvs = new HashMap<String, CustomInventory>();
	
    public File invsFile = new File(getDataFolder()+"/inventories.yml");
    public FileConfiguration invsData = YamlConfiguration.loadConfiguration(invsFile);
    
	InventoryStringDeSerializer serializer = new InventoryStringDeSerializer();
	
	private ItemStack nextItem;

    // ======================
    // Enable
    // ======================
    public void onEnable() {
        saveDefaultConfig();
        saveInvsFile();
        
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        
        if (getConfig().getBoolean("metrics")) {
            try {
                MetricsLite metrics = new MetricsLite(this);
                metrics.start();
                Bukkit.getServer().getLogger().info("[InventoryPages] Metrics Enabled!");
            } catch (IOException e) {
                Bukkit.getServer().getLogger().info("[InventoryPages] Failed to Start Metrics.");
            }
        } else {
            Bukkit.getServer().getLogger().info("[InventoryPages] Metrics Disabled.");
        }
        
        // initialize next item
        initNextItem();
        
        // load all online players into hashmap
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
        	try {
        		loadInvFromFileIntoHashMap(player);
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
        
        Bukkit.getServer().getLogger().info("[InventoryPages] Plugin Enabled!");
    }

	// ======================
    // Disable
    // ======================
    public void onDisable() {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
        	// update inventories to hashmap and save to file
        	updateInvToHashMap(player);
			saveInvFromHashMapToFile(player);
        }
        Bukkit.getServer().getLogger().info("[InventoryPages] Plugin Disabled!");
    }
    
	// ======================
    // Initialize Next Item
    // ======================
    public void initNextItem() {
    	nextItem = new ItemStack(Material.DIAMOND);
        ItemMeta nextItemMeta = nextItem.getItemMeta();
        nextItemMeta.setDisplayName("Next");
        nextItem.setItemMeta(nextItemMeta);
    }
    
	// =========================
    // Save Inventory From HashMap To File
    // =========================
	public void saveInvFromHashMapToFile(Player player) {
		String playerName = player.getName();
		if (playerInvs.containsKey(playerName)) {
	    	for(Entry<Integer, ItemStack[]> pageItemEntry : playerInvs.get(playerName).getItems().entrySet()) {
	    		for(int i = 0; i < pageItemEntry.getValue().length; i++) {
	        		invsData.set(playerName + "." + pageItemEntry.getKey() + "." + i, InventoryStringDeSerializer.toBase64(pageItemEntry.getValue()[i]));
	    		}
	    	}
	    	saveInvsFile();
		}
	}
	
	// =========================
    // Load Inventory From File Intro HashMap
    // =========================
	@SuppressWarnings("deprecation")
	public void loadInvFromFileIntoHashMap(Player player) throws IOException {
		String playerName = player.getName();
    	CustomInventory inventory = new CustomInventory(player, nextItem);
    
    	if(invsData.contains(playerName)) {
    		HashMap<Integer, ItemStack[]> pageItemHashMap = new HashMap<Integer, ItemStack[]>();

        	int pageNum = 0;
        	Boolean pageExists = invsData.contains(playerName + "." + pageNum);

        	Bukkit.getLogger().info("Starting Loop + Page Exists: " + pageExists);
        	while (pageExists) {
        		Bukkit.getLogger().info("Loading " + playerName + "'s Page: " + pageNum);
        		ItemStack[] pageItems = new ItemStack[27];
        		for(int i = 0; i < pageItems.length; i++) {
        			ItemStack item = InventoryStringDeSerializer.stacksFromBase64(invsData.getString(playerName + "." + pageNum + "." + i))[0];
        			if (item != null) {
        				Bukkit.getLogger().info("Valid item: " + item.getTypeId());
        				pageItems[i] = item;
        			}
        		}
        		pageItemHashMap.put(pageNum, pageItems);
        		
        		pageNum++;
        		pageExists = invsData.contains(playerName + "." + pageNum);
        	}
        	inventory.setItems(pageItemHashMap);

    	} else {
    		// TODO player has no inventory in file or hashmap
    		// create a new inventory
    		inventory.saveCurrentPage();
    	}
    	playerInvs.put(playerName, inventory);
    	playerInvs.get(playerName).showPage(0);
	}
	
	// =========================
    // Update Inventory To HashMap
    // =========================
	public void updateInvToHashMap(Player player) {
		String playerName = player.getName();
		if(playerInvs.containsKey(playerName)) {
			playerInvs.get(playerName).saveCurrentPage();
		} else {
			// TODO player has no inventory in hashmap
			// create inventory and save to hashmap
		}
	}
	
	// =========================
    // Remove Inventory From HashMap
    // =========================
	public void removeInvFromHashMap(Player player) {
		String playerName = player.getName();
		if(playerInvs.containsKey(playerName)) {
			playerInvs.remove(playerName);
		}
	}
	
	// =========================
    // Save Inventory File
    // =========================
    public void saveInvsFile() {
        try {
        	invsData.save(invsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	// =========================
    // Login
    // =========================
    @EventHandler
    public void playerJoin(PlayerJoinEvent event) throws InterruptedException, IOException {
    	Player player = event.getPlayer();
    	loadInvFromFileIntoHashMap(player);
    }
    
    // =========================
    // Logout
    // =========================
    @EventHandler
    public void playerQuit(PlayerQuitEvent event) throws InterruptedException {
    	Player player = event.getPlayer();
    	updateInvToHashMap(player);
    	saveInvFromHashMapToFile(player);
    	removeInvFromHashMap(player);
    }
    
    // =========================
    // Death
    // =========================
    public void onDeath(PlayerDeathEvent event) {
    	List<ItemStack> drops = event.getDrops();
    	if(drops.contains(nextItem)) {
    		event.getDrops().remove(nextItem);
    	}
    	
    }
    
    // =========================
    // Respawn
    // =========================
    public void onRespawn(PlayerRespawnEvent event) {
    	Player player = event.getPlayer();
    	String playerName = player.getName();
    	playerInvs.get(playerName).showPage();
    }
    
//    // =========================
//    // Open Inventory
//    // =========================
//    @EventHandler
//    public void onInventoryOpen(InventoryOpenEvent event){
//    }
    
    // =========================
    // Inventory Click
    // =========================
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
		HumanEntity human = event.getWhoClicked();
		if (human instanceof Player) {
			Player player = (Player) human;
			int slot = event.getRawSlot();
	    	if (slot == 35) {
	    		event.setCancelled(true);
	    		playerInvs.get(player.getName()).nextPage();
	    	} else if (slot == 27) {
	    		event.setCancelled(true);
	    		playerInvs.get(player.getName()).prevPage();
	    	}
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