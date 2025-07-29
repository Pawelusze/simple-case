package pl.polardev.scase.models;

import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class Crate {
    private String name;
    private ItemStack displayItem;
    private ItemStack keyItem;
    private List<ItemStack> items;

    public Crate(String name, ItemStack displayItem) {
        this.name = name;
        this.displayItem = displayItem.clone();
        this.items = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public ItemStack getDisplayItem() {
        return displayItem.clone();
    }

    public void setDisplayItem(ItemStack displayItem) {
        this.displayItem = displayItem.clone();
    }

    public ItemStack getKeyItem() {
        return keyItem != null ? keyItem.clone() : null;
    }

    public void setKeyItem(ItemStack keyItem) {
        this.keyItem = keyItem != null ? keyItem.clone() : null;
    }

    public List<ItemStack> getItems() {
        return new ArrayList<>(items);
    }

    public void addItem(ItemStack item) {
        items.add(item.clone());
    }

    public void removeItem(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
        }
    }

    public void setItems(List<ItemStack> items) {
        this.items = new ArrayList<>();
        for (ItemStack item : items) {
            if (item != null) {
                this.items.add(item.clone());
            }
        }
    }

    public boolean hasKey() {
        return keyItem != null;
    }

    public ItemStack getRandomItem() {
        if (items.isEmpty()) return null;
        return items.get((int) (Math.random() * items.size())).clone();
    }
}
