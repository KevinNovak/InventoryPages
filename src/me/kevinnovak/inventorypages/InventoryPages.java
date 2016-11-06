package me.kevinnovak.inventorypages;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class InventoryPages extends JavaPlugin implements Listener{
	private HashMap<String, CustomInventory> playerInvs = new HashMap<String, CustomInventory>();
    ColorConverter colorConv = new ColorConverter(this);
	
	private ItemStack nextItem, prevItem, noActionItem;
	private Integer prevPos, nextPos;

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
                Bukkit.getServer().getLogger().info("[InventoryPages] Metrics Enabled!");
            } catch (IOException e) {
                Bukkit.getServer().getLogger().info("[InventoryPages] Failed to Start Metrics.");
            }
        } else {
            Bukkit.getServer().getLogger().info("[InventoryPages] Metrics Disabled.");
        }
        
        // initialize next, prev items
        initItems();
        
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
    @SuppressWarnings("deprecation")
	public void initItems() {
    	prevItem = new ItemStack(getConfig().getInt("items.prev.ID"), 1, (short) getConfig().getInt("items.prev.variation"));
        ItemMeta prevItemMeta = prevItem.getItemMeta();
        prevItemMeta.setDisplayName(colorConv.convertConfig("items.prev.name"));
        prevItemMeta.setLore(colorConv.convertConfigList("items.prev.lore"));
        prevItem.setItemMeta(prevItemMeta);
        
        prevPos = getConfig().getInt("items.prev.position");
    	
        nextItem = new ItemStack(getConfig().getInt("items.next.ID"), 1, (short) getConfig().getInt("items.next.variation"));
        ItemMeta nextItemMeta = nextItem.getItemMeta();
        nextItemMeta.setDisplayName(colorConv.convertConfig("items.next.name"));
        nextItemMeta.setLore(colorConv.convertConfigList("items.next.lore"));
        nextItem.setItemMeta(nextItemMeta);
        
        nextPos = getConfig().getInt("items.next.position");
        
        noActionItem = new ItemStack(getConfig().getInt("items.noAction.ID"), 1, (short) getConfig().getInt("items.noAction.variation"));
        ItemMeta noActionItemMeta = noActionItem.getItemMeta();
        noActionItemMeta.setDisplayName(colorConv.convertConfig("items.noAction.name"));
        noActionItemMeta.setLore(colorConv.convertConfigList("items.noAction.lore"));
        noActionItem.setItemMeta(noActionItemMeta);
    }
    
	// =========================
    // Save Inventory From HashMap To File
    // =========================
	public void saveInvFromHashMapToFile(Player player) {
		String playerUUID = player.getUniqueId().toString();
		if (playerInvs.containsKey(playerUUID)) {
			File playerFile = new File (getDataFolder() + "/inventories/" + playerUUID + ".yml");
			FileConfiguration playerData = YamlConfiguration.loadConfiguration(playerFile);
			
	    	for(Entry<Integer, ArrayList<ItemStack>> pageItemEntry : playerInvs.get(playerUUID).getItems().entrySet()) {
	    		for(int i = 0; i < pageItemEntry.getValue().size(); i++) {
	    			playerData.set(playerUUID + "." + pageItemEntry.getKey() + "." + i, InventoryStringDeSerializer.toBase64(pageItemEntry.getValue().get(i)));
	    		}
	    	}
	    	
	    	try {
				playerData.save(playerFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	// =========================
    // Load Inventory From File Intro HashMap
    // =========================
	public void loadInvFromFileIntoHashMap(Player player) throws IOException {
		String playerUUID = player.getUniqueId().toString();
    	CustomInventory inventory = new CustomInventory(player, prevItem, prevPos, nextItem, nextPos, noActionItem);
    
		File playerFile = new File (getDataFolder() + "/inventories/" + playerUUID + ".yml");
		FileConfiguration playerData = YamlConfiguration.loadConfiguration(playerFile);
		
    	if(playerFile.exists() && playerData.contains(playerUUID)) {
    		HashMap<Integer, ArrayList<ItemStack>> pageItemHashMap = new HashMap<Integer, ArrayList<ItemStack>>();

        	int pageNum = 0;
        	Boolean pageExists = playerData.contains(playerUUID + "." + pageNum);

        	Bukkit.getLogger().info("Starting Loop + Page Exists: " + pageExists);
        	while (pageExists) {
        		Bukkit.getLogger().info("Loading " + playerUUID + "'s Page: " + pageNum);
        		ArrayList<ItemStack> pageItems = new ArrayList<ItemStack>(25);
        		for(int i = 0; i < 25; i++) {
        			ItemStack item = InventoryStringDeSerializer.stacksFromBase64(playerData.getString(playerUUID + "." + pageNum + "." + i))[0];
        			pageItems.add(item);
        		}
        		pageItemHashMap.put(pageNum, pageItems);
        		
        		pageNum++;
        		pageExists = playerData.contains(playerUUID + "." + pageNum);
        	}
        	inventory.setItems(pageItemHashMap);

    	}
    	playerInvs.put(playerUUID, inventory);
    	playerInvs.get(playerUUID).showPage(0);
	}
	
	// =========================
    // Update Inventory To HashMap
    // =========================
	public void updateInvToHashMap(Player player) {
		String playerUUID = player.getUniqueId().toString();
		if(playerInvs.containsKey(playerUUID)) {
			playerInvs.get(playerUUID).saveCurrentPage();
		} else {
			// TODO player has no inventory in hashmap
			// create inventory and save to hashmap
		}
	}
	
	// =========================
    // Remove Inventory From HashMap
    // =========================
	public void removeInvFromHashMap(Player player) {
		String playerUUID = player.getUniqueId().toString();
		if(playerInvs.containsKey(playerUUID)) {
			playerInvs.remove(playerUUID);
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
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
    	Player player = event.getEntity();
    	
    	//save items before death
    	updateInvToHashMap(player);
    	
    	List<ItemStack> drops = event.getDrops();
        event.setKeepLevel(true);
        ListIterator<ItemStack> litr = drops.listIterator();
        while(litr.hasNext()){
        	ItemStack stack = litr.next();
        if (stack.getType() == prevItem.getType() && stack.getItemMeta().getDisplayName() == prevItem.getItemMeta().getDisplayName()) {
            litr.remove();
        } else if (stack.getType() == nextItem.getType() && stack.getItemMeta().getDisplayName() == nextItem.getItemMeta().getDisplayName()) {
        	litr.remove();
        } else if (stack.getType() == noActionItem.getType() && stack.getItemMeta().getDisplayName() == noActionItem.getItemMeta().getDisplayName()) {
        	litr.remove();
        }
    }
    }
    
    // =========================
    // Respawn
    // =========================
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
    	Player player = event.getPlayer();
    	String playerUUID = player.getUniqueId().toString();
    	
    	// saves empty inventory (other than next and prev)
    	// disable this if you want to keep items
    	updateInvToHashMap(player);
    	
    	playerInvs.get(playerUUID).showPage();
    }
    
    // =========================
    // Inventory Click
    // =========================
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
		HumanEntity human = event.getWhoClicked();
		if (human instanceof Player) {
			Player player = (Player) human;
			String playerUUID = (String) player.getUniqueId().toString();
			int slot = event.getSlot();
	    	if (slot == prevPos+9) {
	    		event.setCancelled(true);
	    		playerInvs.get(playerUUID).prevPage();
	    	} else if (slot == nextPos+9) {
	    		event.setCancelled(true);
	    		playerInvs.get(playerUUID).nextPage();
	    	}
		}
    }
    
    // =========================
    // Inventory Pickup
    // =========================
    @EventHandler
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
		ItemStack item = event.getItem().getItemStack();
        if (item.getType() == prevItem.getType() && item.getItemMeta().getDisplayName() == prevItem.getItemMeta().getDisplayName()) {
            event.setCancelled(true);
        } else if (item.getType() == nextItem.getType() && item.getItemMeta().getDisplayName() == nextItem.getItemMeta().getDisplayName()) {
        	event.setCancelled(true);
        } else if (item.getType() == noActionItem.getType() && item.getItemMeta().getDisplayName() == noActionItem.getItemMeta().getDisplayName()) {
        	event.setCancelled(true);
        }
    }
    
    // =========================
    // Item Drop
    // =========================
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
		ItemStack item = event.getItemDrop().getItemStack();
        if (item.getType() == prevItem.getType() && item.getItemMeta().getDisplayName() == prevItem.getItemMeta().getDisplayName()) {
            event.setCancelled(true);
        } else if (item.getType() == nextItem.getType() && item.getItemMeta().getDisplayName() == nextItem.getItemMeta().getDisplayName()) {
        	event.setCancelled(true);
        } else if (item.getType() == noActionItem.getType() && item.getItemMeta().getDisplayName() == noActionItem.getItemMeta().getDisplayName()) {
        	event.setCancelled(true);
        }
    }
    
//    // =========================
//    // Inventory Drag
//    // =========================
//    @EventHandler
//    public void onInventoryDrag(InventoryDragEvent event) {
//		Set<Integer> slots = event.getInventorySlots();
//		for (Integer slot : slots) {
//			event.getWhoClicked().sendMessage("Dragging Slot: " + slot);
//		}
//    	if (slots.contains(27) || slots.contains(35)) {
//    		event.setCancelled(true);
//    	}
//    }
//    
//    // =========================
//    // Inventory Creative
//    // =========================
//    @EventHandler
//    public void onInventoryCreative(InventoryCreativeEvent event) {
//		int slot = event.getSlot();
//		event.getWhoClicked().sendMessage("Clicked: " + slot);
//		if (event.getCurrentItem() == nextItem) {
//			event.setCancelled(true);
//		}
//    	if (slot == 35) {
//    		event.setCancelled(true);
//    	} else if (slot == 27) {
//    		event.setCancelled(true);
//    	}
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
		String playerUUID = player.getUniqueId().toString();
        // ======================
        // Player
        // ======================
        if (cmd.getName().equalsIgnoreCase("testsave")) {
        	playerInvs.get(playerUUID).saveCurrentPage();
        }
        if (cmd.getName().equalsIgnoreCase("testnext")) {
        	playerInvs.get(playerUUID).nextPage();
        }
        if (cmd.getName().equalsIgnoreCase("testprev")) {
        	playerInvs.get(playerUUID).prevPage();
        }
		return true;
    }
}