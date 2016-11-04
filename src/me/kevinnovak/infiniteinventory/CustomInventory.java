package me.kevinnovak.infiniteinventory;

import java.util.HashMap;

import org.bukkit.Bukkit;
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
		this.savePage();
	}
	
	void setPlayer(Player player) {
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
			this.player.getInventory().setItem(i+9, items.get(this.page).get(i));
		}
		this.player.sendMessage("After Show - Page: " + this.page);
	}
	
	void nextPage() {
		this.savePage();
		this.page = this.page + 1;
		if (!pageExists(this.page)) {
			createPage(this.page);
		}
		this.showPage();
		this.savePage();
		this.player.sendMessage("After Next - Page: " + this.page);
		Bukkit.getLogger().info("Player: " + player.getName());
		Bukkit.getLogger().info("Page: " + this.page);
	}
	
	void prevPage() {
		if (this.page > 0) {
			this.savePage();
			this.page = this.page - 1;
			this.showPage();
			this.savePage();
		}
		this.player.sendMessage("After Prev - Page: " + this.page);
	}
	
	
}