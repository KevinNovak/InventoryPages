package me.kevinnovak.inventorypages;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CustomInventory {
    private Player player;
    private ItemStack prevItem, nextItem, noActionItem;
    private Integer page = 0, maxPage = 1, prevPos, nextPos;
    private HashMap < Integer, ArrayList < ItemStack >> items = new HashMap < Integer, ArrayList < ItemStack >> ();;

    // ======================================
    // Constructor
    // ======================================
    CustomInventory(Player player, int maxPage, ItemStack prevItem, Integer prevPos, ItemStack nextItem, Integer nextPos, ItemStack noActionItem) {
        this.player = player;
        this.maxPage = maxPage;
        this.prevItem = prevItem;
        this.prevPos = prevPos;
        this.nextItem = nextItem;
        this.nextPos = nextPos;
        this.noActionItem = noActionItem;

        // create pages
        for (int i = 0; i < maxPage + 1; i++) {
            if (!pageExists(i)) {
                createPage(i);
            }
        }

        saveCurrentPage();

        // check for items in essential slots
        ItemStack itemInPrevItemSlot = this.player.getInventory().getItem(prevPos + 9);
        ItemStack itemInNextItemSlot = this.player.getInventory().getItem(nextPos + 9);
        if (itemInPrevItemSlot != null) {
            if (itemInPrevItemSlot.getType() != prevItem.getType() && itemInPrevItemSlot.getItemMeta().getDisplayName() != prevItem.getItemMeta().getDisplayName()) {
                if (itemInPrevItemSlot.getType() != noActionItem.getType() && itemInPrevItemSlot.getItemMeta().getDisplayName() != noActionItem.getItemMeta().getDisplayName()) {
                    SimpleEntry < Integer, Integer > nextFreeSpace = nextFreeSpace();
                    this.items.get(nextFreeSpace.getKey()).set(nextFreeSpace.getValue(), itemInPrevItemSlot);
                    this.player.getInventory().setItem(prevPos, null);
                }
            }
        }
        if (itemInNextItemSlot != null) {
            if (itemInNextItemSlot.getType() != nextItem.getType() && itemInNextItemSlot.getItemMeta().getDisplayName() != nextItem.getItemMeta().getDisplayName()) {
                if (itemInNextItemSlot.getType() != noActionItem.getType() && itemInNextItemSlot.getItemMeta().getDisplayName() != noActionItem.getItemMeta().getDisplayName()) {
                    SimpleEntry < Integer, Integer > nextFreeSpace = nextFreeSpace();
                    this.items.get(nextFreeSpace.getKey()).set(nextFreeSpace.getValue(), itemInNextItemSlot);
                    this.player.getInventory().setItem(nextPos, null);
                }
            }
        }

        player.sendMessage("Your max pages are: " + (maxPage + 1));
    }

    // ======================================
    // Save Current Page
    // ======================================
    void saveCurrentPage() {
        if (player.getGameMode() != GameMode.CREATIVE) {
            ArrayList < ItemStack > pageItems = new ArrayList < ItemStack > (25);
            for (int i = 0; i < 27; i++) {
                if (i != prevPos && i != nextPos) {
                    pageItems.add(this.player.getInventory().getItem(i + 9));
                }
            }
            this.items.put(this.page, pageItems);
        }
    }

    // ======================================
    // Show Page
    // ======================================
    void showPage() {
        this.showPage(this.page);
    }

    void showPage(Integer page) {
        showPage(page, GameMode.SURVIVAL);
    }

    void showPage(GameMode gm) {
        showPage(this.page, gm);
    }

    void showPage(Integer page, GameMode gm) {
        this.page = page;
        player.sendMessage("GameMode: " + gm);
        Boolean foundPrev = false;
        Boolean foundNext = false;
        for (int i = 0; i < 27; i++) {
            int j = i;
            if (i == prevPos) {
                if (page == 0) {
                    if (gm != GameMode.CREATIVE) {
                        this.player.getInventory().setItem(i + 9, addPageNums(noActionItem));
                    } else {
                        this.player.getInventory().setItem(i + 9, null);
                    }
                } else {
                    if (gm != GameMode.CREATIVE) {
                        this.player.getInventory().setItem(i + 9, addPageNums(prevItem));
                    } else {
                        this.player.getInventory().setItem(i + 9, null);
                    }
                }
                foundPrev = true;
            } else if (i == nextPos) {
                if (page == maxPage) {
                    if (gm != GameMode.CREATIVE) {
                        this.player.getInventory().setItem(i + 9, addPageNums(noActionItem));
                    } else {
                        this.player.getInventory().setItem(i + 9, null);
                    }
                } else {
                    if (gm != GameMode.CREATIVE) {
                        this.player.getInventory().setItem(i + 9, addPageNums(nextItem));
                    } else {
                        this.player.getInventory().setItem(i + 9, null);
                    }
                }
                foundNext = true;
            } else {
                if (foundPrev) {
                    j--;
                }
                if (foundNext) {
                    j--;
                }
                this.player.getInventory().setItem(i + 9, this.items.get(page).get(j));
            }
        }
        player.sendMessage("Showing Page: " + page);
    }

    // ======================================
    // Add Page Numbers
    // ======================================
    ItemStack addPageNums(ItemStack item) {
        ItemStack modItem = new ItemStack(item);
        ItemMeta itemMeta = modItem.getItemMeta();
        List < String > itemLore = itemMeta.getLore();
        for (int j = 0; j < itemLore.size(); j++) {
            Integer currentPageUser = page + 1;
            Integer maxPageUser = maxPage + 1;
            itemLore.set(j, itemLore.get(j).replace("{CURRENT}", currentPageUser.toString()).replace("{MAX}", maxPageUser.toString()));
        }
        itemMeta.setLore(itemLore);
        modItem.setItemMeta(itemMeta);
        return modItem;
    }

    // ======================================
    // Previous Page
    // ======================================
    void prevPage() {
        if (this.page > 0) {
            this.saveCurrentPage();
            this.page = this.page - 1;
            this.showPage();
            this.saveCurrentPage();
        }
    }

    // ======================================
    // Next Page
    // ======================================
    void nextPage() {
        if (this.page < maxPage) {
            this.saveCurrentPage();
            this.page = this.page + 1;
            this.showPage();
            this.saveCurrentPage();
        }
    }

    // ======================================
    // Page Exists
    // ======================================
    Boolean pageExists(Integer page) {
        if (items.containsKey(page)) {
            return true;
        }
        return false;
    }

    // ======================================
    // Create Page
    // ======================================
    void createPage(Integer page) {
        ArrayList < ItemStack > pageItems = new ArrayList < ItemStack > (25);
        for (int i = 0; i < 25; i++) {
            pageItems.add(null);
        }
        this.items.put(page, pageItems);
    }

    // ======================================
    // Get/Set Items
    // ======================================
    HashMap < Integer, ArrayList < ItemStack >> getItems() {
        return this.items;
    }

    void setItems(HashMap < Integer, ArrayList < ItemStack >> items) {
        this.items = items;
    }

    // ======================================
    // Next Free Space
    // ======================================
    SimpleEntry < Integer, Integer > nextFreeSpace() {
        for (Integer i = 0; i < maxPage + 1; i++) {
            for (Integer j = 0; j < 25; j++) {
                if (items.get(i).get(j) == null) {
                    SimpleEntry < Integer, Integer > pageAndPos = new AbstractMap.SimpleEntry < Integer, Integer > (i, j);
                    return pageAndPos;
                }
            }
        }
        return null;
    }
}