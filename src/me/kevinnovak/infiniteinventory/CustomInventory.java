package me.kevinnovak.infiniteinventory;

import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CustomInventory {
	private Player player;
//	private ItemStack nextItem;
//	private ItemStack prevItem;
	private Integer page = 0;
	private HashMap<Integer, HashMap<Integer, ItemStack>> items = new HashMap<Integer, HashMap<Integer, ItemStack>>();;
	
	CustomInventory(Player player) {
		this.player = player;
	}
	
	void savePage() {
		HashMap<Integer, ItemStack> pageItems = new HashMap<Integer, ItemStack>();
		for(int i=0; i<27; i++) {
			pageItems.put(i, this.player.getInventory().getItem(i+9));
		}
		this.items.put(this.page, pageItems);
	}
	
	void createPage(Integer page) {
		HashMap<Integer, ItemStack> pageItems = new HashMap<Integer, ItemStack>();
		for(int i=0; i<27; i++) {
			pageItems.put(i, null);
		}
		this.items.put(page, pageItems);
	}
	
	Boolean pageExists(Integer page) {
		HashMap<Integer, ItemStack> pageItems = this.items.get(page);
		if (pageItems != null) {
		    return true;
		}
		return false;
	}
	
	void showPage() {
		for(int i=0; i<27; i++) {
			this.player.getInventory().setItem(i+9, items.get(this.page).get(i));
		}
	}
	
	void nextPage() {
		this.savePage();
		this.page = this.page + 1;
		if (!pageExists(this.page)) {
			createPage(this.page);
		}
		this.showPage();
	}
	
	void prevPage() {
		this.savePage();
		if (this.page > 0) {
			this.page = this.page - 1;
		}
		this.showPage();
	}
	
	
}