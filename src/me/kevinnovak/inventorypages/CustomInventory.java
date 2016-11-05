package me.kevinnovak.inventorypages;

import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CustomInventory {
	private Player player;
	private ItemStack prevItem, nextItem, noActionItem;
	private Integer page = 0;
	private HashMap<Integer, ItemStack[]> items = new HashMap<Integer, ItemStack[]>();;
	
	CustomInventory(Player player, ItemStack prevItem, ItemStack nextItem, ItemStack noActionItem) {
		this.player = player;
		this.prevItem = prevItem;
		this.nextItem = nextItem;
		this.noActionItem = noActionItem;
		this.saveCurrentPage();
		ItemStack itemInPrevItemSlot = this.items.get(0)[18];
		ItemStack itemInNextItemSlot = this.items.get(0)[26];
		if(itemInPrevItemSlot != prevItem && itemInPrevItemSlot != noActionItem && itemInPrevItemSlot != null) {
			if (!pageExists(1)) {
				createPage(1);
			}
			this.items.get(1)[0] = itemInPrevItemSlot;
		}
		if(itemInNextItemSlot != nextItem && itemInNextItemSlot != noActionItem && itemInNextItemSlot != null) {
			if (!pageExists(1)) {
				createPage(1);
			}
			this.items.get(1)[1] = itemInNextItemSlot;
		}
	}
	
	void setPlayer(Player player) {
		this.player = player;
	}
	
	void saveCurrentPage() {
		ItemStack[] pageItems = new ItemStack[27];
		for(int i=0; i<27; i++) {
			pageItems[i] = this.player.getInventory().getItem(i+9);
		}
		this.items.put(this.page, pageItems);
	}
	
	void showPage() {
		this.showPage(this.page);
	}
	
	void showPage(Integer page) {
		this.page = page;
		for(int i=0; i<27; i++) {
			this.player.getInventory().setItem(i+9, items.get(this.page)[i]);
			if (i== 18) {
				if(page == 0) {
					this.player.getInventory().setItem(i+9, noActionItem);
				} else {
					this.player.getInventory().setItem(i+9, prevItem);
				}
			} else if (i == 26) {
				this.player.getInventory().setItem(i+9, nextItem);
			}
		}
		player.sendMessage("Showing Page: " + page);
	}
	
	void nextPage() {
		this.saveCurrentPage();
		this.page = this.page + 1;
		if (!pageExists(this.page)) {
			createPage(this.page);
		}
		this.showPage();
		this.saveCurrentPage();
	}
	
	Boolean pageExists(Integer page) {
		if (items.containsKey(page)) {
		    return true;
		}
		return false;
	}
	
	void createPage(Integer page) {
		ItemStack[] pageItems = new ItemStack[27];
		for(int i=0; i<27; i++) {
			pageItems[i] = null;
		}
		this.items.put(page, pageItems);
	}
	
	void prevPage() {
		if (this.page > 0) {
			this.saveCurrentPage();
			this.page = this.page - 1;
			this.showPage();
			this.saveCurrentPage();
		}
	}
	
	HashMap<Integer, ItemStack[]> getItems() {
		return this.items;
	}
	
	void setItems(HashMap<Integer, ItemStack[]> items) {
		this.items = items;
	}
	
}