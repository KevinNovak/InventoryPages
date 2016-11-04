package me.kevinnovak.infiniteinventory;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class InfiniteInventory extends JavaPlugin implements Listener{
	private HashMap<String, CustomInventory> playerInvs = new HashMap<String, CustomInventory>();
	InventoryStringDeSerializer serializer = new InventoryStringDeSerializer();
    public File invsFile = new File(getDataFolder()+"/inventories.yml");
    public FileConfiguration invsData = YamlConfiguration.loadConfiguration(invsFile);
	
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
                Bukkit.getServer().getLogger().info("[InfiniteInventory] Metrics Enabled!");
            } catch (IOException e) {
                Bukkit.getServer().getLogger().info("[InfiniteInventory] Failed to Start Metrics.");
            }
        } else {
            Bukkit.getServer().getLogger().info("[InfiniteInventory] Metrics Disabled.");
        }
        Bukkit.getServer().getLogger().info("[InfiniteInventory] Plugin Enabled!");
        
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
        	try {
				loadInv(player);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }

	// ======================
    // Disable
    // ======================
    public void onDisable() {
    	saveAllInvs();
        Bukkit.getServer().getLogger().info("[InfiniteInventory] Plugin Disabled!");
    }
    
    private void saveAllInvs() {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
        	playerInvs.get(player.getName()).saveCurrentPage();
        }
    	for(Map.Entry<String, CustomInventory> playerInv : playerInvs.entrySet()) {
    		saveInv(playerInv.getKey());
    	}
    }
    
    private void saveInv(String player) {
    	for(Entry<Integer, ItemStack[]> page : playerInvs.get(player).getItems().entrySet()) {
    		for(int i = 0; i < page.getValue().length; i++) {
        		invsData.set(player + "." + page.getKey() + "." + i, InventoryStringDeSerializer.toBase64(page.getValue()[i]));
    		}
    	}
    	saveInvsFile();
	}
    
    public void saveInvsFile() {
        try {
        	invsData.save(invsFile);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }
    
    @SuppressWarnings("deprecation")
	public void loadInv(Player player) throws IOException {
    	HashMap<Integer, ItemStack[]> items = new HashMap<Integer, ItemStack[]>();
    	CustomInventory inventory = new CustomInventory(player);
    	int pageNum = 0;
    	Boolean pageExists = invsData.contains(player.getName() + "." + pageNum);
    	Bukkit.getLogger().info("Starting Loop + Page Exists: " + pageExists);
    	while (pageExists) {
    		Bukkit.getLogger().info("Loading " + player.getName() + "'s Page: " + pageNum);
    		ItemStack[] pageItems = new ItemStack[27];
    		for(int i = 0; i < 27; i++) {
    			ItemStack item = InventoryStringDeSerializer.stacksFromBase64(invsData.getString(player.getName() + "." + pageNum + "." + i))[0];
    			if (item != null) {
    				Bukkit.getLogger().info("Valid item: " + item.getTypeId());
    				pageItems[i] = item;
    			}
    		}
    		items.put(pageNum, pageItems);
    		pageNum++;
    		pageExists = invsData.contains(player.getName() + "." + pageNum);
    	}
    	inventory.setItems(items);
    	playerInvs.put(player.getName(), inventory);
    	playerInvs.get(player.getName()).showPage(0);
    }

	// =========================
    // Login
    // =========================
    @EventHandler
    public void playerJoin(PlayerJoinEvent event) throws InterruptedException, IOException {
    	Player player = event.getPlayer();
    	
    	//TEMP:
    	loadInv(player);
    	playerInvs.get(player.getName()).setPlayer(player);
    	playerInvs.get(player.getName()).showPage(0);
    	
    	if (!(playerInvs.containsKey(player.getName()) && invsData.contains(player.getName()))) {
    		CustomInventory playerInv = new CustomInventory(player);
    		playerInvs.put(player.getName(), playerInv);
    	} else {
    		loadInv(player);
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