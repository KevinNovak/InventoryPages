package me.kevinnovak.inventorypages;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public class InventoryPages extends JavaPlugin implements Listener {
    File crashedFile = new File(getDataFolder() + "/backups/crashed.yml");
    FileConfiguration crashedData = YamlConfiguration.loadConfiguration(crashedFile);

    private HashMap < String, CustomInventory > playerInvs = new HashMap < String, CustomInventory > ();
    ColorConverter colorConv = new ColorConverter(this);

    private ItemStack nextItem, prevItem, noPageItem;
    private Integer prevPos, nextPos;
    private List < String > clearCommands;

    // ======================================
    // Enable
    // ======================================
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

        // initialize commands
        initCommands();

        // load all online players into hashmap
        for (Player player: Bukkit.getServer().getOnlinePlayers()) {
            try {
                loadInvFromFileIntoHashMap(player);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (getConfig().getBoolean("saving.enabled")) {
            startSaving();
        }

        Bukkit.getServer().getLogger().info("[InventoryPages] Plugin Enabled!");
    }

    // ======================================
    // Disable
    // ======================================
    public void onDisable() {
        for (Player player: Bukkit.getServer().getOnlinePlayers()) {
            String playerUUID = player.getUniqueId().toString();
            if (playerInvs.containsKey(playerUUID)) {
                // update inventories to hashmap and save to file
                updateInvToHashMap(player);
                saveInvFromHashMapToFile(player);
                clearAndRemoveCrashedPlayer(player);
            }
        }
        Bukkit.getServer().getLogger().info("[InventoryPages] Plugin Disabled!");
    }

    // ======================================
    // Update and Save All Inventories to Files
    // ======================================
    public void updateAndSaveAllInventoriesToFiles() {
        if (!Bukkit.getServer().getOnlinePlayers().isEmpty()) {
            for (Player player: Bukkit.getServer().getOnlinePlayers()) {
                String playerUUID = player.getUniqueId().toString();
                if (playerInvs.containsKey(playerUUID)) {
                    updateInvToHashMap(player);
                    saveInvFromHashMapToFile(player);
                }
            }
        }
    }

    // ======================================
    // Initialize Next Item
    // ======================================
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

        noPageItem = new ItemStack(getConfig().getInt("items.noPage.ID"), 1, (short) getConfig().getInt("items.noPage.variation"));
        ItemMeta noPageItemMeta = noPageItem.getItemMeta();
        noPageItemMeta.setDisplayName(colorConv.convertConfig("items.noPage.name"));
        noPageItemMeta.setLore(colorConv.convertConfigList("items.noPage.lore"));
        noPageItem.setItemMeta(noPageItemMeta);
    }

    public void initCommands() {
        clearCommands = getConfig().getStringList("commands.clear.aliases");
    }

    public void startSaving() {
        BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
        	@Override
            public void run() {
                updateAndSaveAllInventoriesToFiles();
            }
        }, 0L, 20 * getConfig().getInt("saving.interval"));
    }

    // ======================================
    // Save Inventory From HashMap To File
    // ======================================
    public void saveInvFromHashMapToFile(Player player) {
        String playerUUID = player.getUniqueId().toString();
        if (playerInvs.containsKey(playerUUID)) {
            File playerFile = new File(getDataFolder() + "/inventories/" + playerUUID.substring(0, 1) + "/" + playerUUID + ".yml");
            FileConfiguration playerData = YamlConfiguration.loadConfiguration(playerFile);

            // save survival items
            for (Entry < Integer, ArrayList < ItemStack >> pageItemEntry: playerInvs.get(playerUUID).getItems().entrySet()) {
                for (int i = 0; i < pageItemEntry.getValue().size(); i++) {
                    if (pageItemEntry.getValue().get(i) != null) {
                        playerData.set("items.main." + pageItemEntry.getKey() + "." + i, InventoryStringDeSerializer.toBase64(pageItemEntry.getValue().get(i)));
                    } else {
                        playerData.set("items.main." + pageItemEntry.getKey() + "." + i, null);
                    }
                }
            }

            // save creative items
            if (playerInvs.get(playerUUID).hasUsedCreative()) {
                for (int i = 0; i < playerInvs.get(playerUUID).getCreativeItems().size(); i++) {
                    if (playerInvs.get(playerUUID).getCreativeItems().get(i) != null) {
                        playerData.set("items.creative.0." + i, InventoryStringDeSerializer.toBase64(playerInvs.get(playerUUID).getCreativeItems().get(i)));
                    } else {
                        playerData.set("items.creative.0." + i, null);
                    }
                }
            }

            // save current page
            playerData.set("page", playerInvs.get(playerUUID).getPage());

            try {
                playerData.save(playerFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // ======================================
    // Load Inventory From File Into HashMap
    // ======================================
    public void loadInvFromFileIntoHashMap(Player player) throws IOException {
        clearAndRemoveCrashedPlayer(player);

        int maxPage = 1;
        Boolean foundPerm = false;
        for (int i = 2; i < 101; i++) {
            if (player.hasPermission("inventorypages.pages." + i)) {
                foundPerm = true;
                maxPage = i - 1;
            }
        }

        if (foundPerm) {
            String playerUUID = player.getUniqueId().toString();
            CustomInventory inventory = new CustomInventory(this, player, maxPage, prevItem, prevPos, nextItem, nextPos, noPageItem);
            playerInvs.put(playerUUID, inventory);
            addCrashedPlayer(player);
            playerInvs.get(playerUUID).showPage(player.getGameMode());
        }
    }

    // ======================================
    // Update Inventory To HashMap
    // ======================================
    public void updateInvToHashMap(Player player) {
        String playerUUID = player.getUniqueId().toString();
        if (playerInvs.containsKey(playerUUID)) {
            playerInvs.get(playerUUID).saveCurrentPage();
        }
    }

    // ======================================
    // Remove Inventory From HashMap
    // ======================================
    public void removeInvFromHashMap(Player player) {
        String playerUUID = player.getUniqueId().toString();
        if (playerInvs.containsKey(playerUUID)) {
            playerInvs.remove(playerUUID);
            clearAndRemoveCrashedPlayer(player);
        }
    }

    // ======================================
    // Save Crashed File
    // ======================================
    public void saveCrashedFile() {
        try {
            crashedData.save(crashedFile);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // ======================================
    // Crashed Players Exist
    // ======================================
    public Boolean crashedPlayersExist() {
        if (crashedData.contains("crashed")) {
            if (crashedData.getConfigurationSection("crashed").getKeys(false).size() > 0) {
                return true;
            }
        }
        return false;
    }

    // ======================================
    // Has Crashed
    // ======================================
    public Boolean hasCrashed(Player player) {
        if (crashedData.contains("crashed." + player.getUniqueId().toString())) {
            return true;
        }
        return false;
    }

    // ======================================
    // Add Crashed Player
    // ======================================
    public void addCrashedPlayer(Player player) {
        crashedData.set("crashed." + player.getUniqueId().toString(), true);
        saveCrashedFile();
    }

    // ======================================
    // Clear and Remove Crashed Player
    // ======================================
    public void clearAndRemoveCrashedPlayer(Player player) {
        if (crashedPlayersExist()) {
            if (hasCrashed(player)) {
                for (int i = 0; i < 27; i++) {
                    player.getInventory().setItem(i + 9, null);
                }
                crashedData.set("crashed." + player.getUniqueId().toString(), null);
                saveCrashedFile();
            }
        }
    }

    // ======================
    // Clear Player Hotbar
    // ======================
    public void clearHotbar(Player player) {
        for (int i = 0; i < 9; i++) {
            player.getInventory().setItem(i, null);
        }
    }

    // ======================================
    // Has Switcher Items
    // ======================================
    public Boolean hasSwitcherItems(Player player) {
        String playerUUID = player.getUniqueId().toString();
        if (playerInvs.containsKey(playerUUID)) {
            if (player.getGameMode() != GameMode.CREATIVE) {
                return true;
            }
        }
        return false;
    }

    // ======================================
    // Is A Switcher Item
    // ======================================
    public Boolean isSwitcherItem(ItemStack item, ItemStack switcherItem) {
        if (item != null) {
            if (item.getType() != null) {
                if (item.getType().equals(switcherItem.getType())) {
                    if (item.getItemMeta() != null) {
                        if (item.getItemMeta().getDisplayName() != null) {
                            if (item.getItemMeta().getDisplayName().equals(switcherItem.getItemMeta().getDisplayName())) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    // ======================================
    // Login
    // ======================================
    @EventHandler
    public void playerJoin(PlayerJoinEvent event) throws InterruptedException, IOException {
        Player player = event.getPlayer();
        loadInvFromFileIntoHashMap(player);
    }

    // ======================================
    // Logout
    // ======================================
    @EventHandler
    public void playerQuit(PlayerQuitEvent event) throws InterruptedException {
        Player player = event.getPlayer();
        String playerUUID = player.getUniqueId().toString();
        if (playerInvs.containsKey(playerUUID)) {
            updateInvToHashMap(player);
            saveInvFromHashMapToFile(player);
            removeInvFromHashMap(player);
        }
    }

    // ======================================
    // Death
    // ======================================
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String playerUUID = player.getUniqueId().toString();
        if (playerInvs.containsKey(playerUUID)) {
            //save items before death
            updateInvToHashMap(player);

            if (hasSwitcherItems(player)) {
                List < ItemStack > drops = event.getDrops();
                event.setKeepLevel(true);
                ListIterator < ItemStack > litr = drops.listIterator();
                while (litr.hasNext()) {
                    ItemStack item = litr.next();
                    if (isSwitcherItem(item, prevItem) || isSwitcherItem(item, nextItem) || isSwitcherItem(item, noPageItem)) {
                        litr.remove();
                    }
                }
            }
        }
    }

    // ======================================
    // Respawn
    // ======================================
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        String playerUUID = player.getUniqueId().toString();
        if (playerInvs.containsKey(playerUUID)) {
            GameMode gm = player.getGameMode();

            // saves empty inventory (other than next and prev)
            // disable this if you want to keep items
            updateInvToHashMap(player);

            playerInvs.get(playerUUID).showPage(gm);
        }
    }

    // ======================================
    // Inventory Click
    // ======================================
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory clickedInv = getClickedInventory(event.getView(), event.getRawSlot());
        if (clickedInv != null) {
            if (clickedInv.getType() == InventoryType.PLAYER) {
                InventoryHolder holder = clickedInv.getHolder();
                if (holder instanceof Player) {
                    Player player = (Player) holder;
                    if (hasSwitcherItems(player)) {
                        ItemStack item = event.getCurrentItem();
                        if (isSwitcherItem(item, prevItem)) {
                            event.setCancelled(true);
                            playerInvs.get(player.getUniqueId().toString()).prevPage();
                        } else if (isSwitcherItem(item, nextItem)) {
                            event.setCancelled(true);
                            playerInvs.get(player.getUniqueId().toString()).nextPage();
                        } else if (isSwitcherItem(item, noPageItem)) {
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

    // ======================================
    // Get Clicked Inventory
    // ======================================
    public Inventory getClickedInventory(InventoryView view, int slot) {

        int topInvSize = view.getTopInventory().getSize();
        if (view.getTopInventory().getType() == InventoryType.PLAYER) {
            int topInvRemainder = topInvSize % 9;
            if (topInvRemainder != 0) {
                topInvSize = topInvSize - topInvRemainder;
            }
        }

        Inventory clickedInventory;
        if (slot < 0) {
            clickedInventory = null;
        } else if (view.getTopInventory() != null && slot < topInvSize) {
            clickedInventory = view.getTopInventory();
        } else {
            clickedInventory = view.getBottomInventory();
        }
        return clickedInventory;
    }

    // ======================================
    // GameMode Change
    // ======================================
    @EventHandler
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        String playerUUID = player.getUniqueId().toString();
        if (playerInvs.containsKey(playerUUID)) {
            playerInvs.get(playerUUID).saveCurrentPage();
            playerInvs.get(playerUUID).showPage(event.getNewGameMode());
        }
    }

    // ======================
    // Commands
    // ======================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String cmdLine = event.getMessage().toLowerCase();
        // clear
        for (String clearCommand: this.clearCommands) {
            if (cmdLine.startsWith("/" + clearCommand + " ") || cmdLine.equalsIgnoreCase("/" + clearCommand)) {
                Player player = event.getPlayer();
                String playerUUID = player.getUniqueId().toString();

                if (playerInvs.containsKey(playerUUID)) {
                    event.setCancelled(true);
                    if (player.hasPermission("inventorypages.clear")) {
                        GameMode gm = player.getGameMode();
                        if (cmdLine.startsWith("/" + clearCommand + " all ") || cmdLine.equalsIgnoreCase("/" + clearCommand + " all")) {
                            playerInvs.get(playerUUID).clearAllPages(gm);
                        } else {
                            playerInvs.get(playerUUID).clearPage(gm);
                        }
                        clearHotbar(player);
                        playerInvs.get(playerUUID).showPage(gm);
                    }
                }
            }
        }
    }
}