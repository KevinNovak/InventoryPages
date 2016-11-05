package me.kevinnovak.inventorypages;

import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CustomInventory {
	private Player player;
	private ItemStack nextItem = new ItemStack(Material.DIAMOND);
	//private ItemStack prevItem;
	private Integer page = 0;
	private HashMap<Integer, ItemStack[]> items = new HashMap<Integer, ItemStack[]>();;
	
	CustomInventory(Player player) {
		this.player = player;
		this.saveCurrentPage();
	}
	
	void setPlayer(Player player) {
		this.player = player;
	}
	
	void createPage(Integer page) {
		ItemStack[] pageItems = new ItemStack[27];
		for(int i=0; i<27; i++) {
			pageItems[i] = null;
		}
		this.items.put(page, pageItems);
	}
	
	void saveCurrentPage() {
		ItemStack[] pageItems = new ItemStack[27];
		for(int i=0; i<27; i++) {
			pageItems[i] = this.player.getInventory().getItem(i+9);
		}
		this.items.put(this.page, pageItems);
	}
	
	Boolean pageExists(Integer page) {
		if (items.containsKey(page)) {
		    return true;
		}
		return false;
	}
	
	void showPage() {
		this.showPage(this.page);
	}
	
	void showPage(Integer page) {
		this.page = page;
		for(int i=0; i<27; i++) {
			this.player.getInventory().setItem(i+9, items.get(this.page)[i]);
			if (i== 18 || i == 26) {
	            ItemMeta nextItemMeta = nextItem.getItemMeta();
	            nextItemMeta.setDisplayName("Next");
	            nextItem.setItemMeta(nextItemMeta);
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