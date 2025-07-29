package pl.polardev.scase.model;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Crate {
    private final String name;
    private ItemStack displayItem;
    private ItemStack keyItem;
    private List<ItemStack> items;
    private static final Random RANDOM = ThreadLocalRandom.current();

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

    public void setItems(List<ItemStack> items) {
        this.items = new ArrayList<>();
        for (ItemStack item : items) {
            if (item != null) {
                this.items.add(item.clone());
            }
        }
    }

    public void addItem(ItemStack item) {
        if (item != null) {
            items.add(item.clone());
        }
    }

    public void removeItem(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
        }
    }

    public ItemStack getRandomItem() {
        if (items.isEmpty()) {
            return null;
        }
        return items.get(RANDOM.nextInt(items.size())).clone();
    }

    public boolean hasItems() {
        return !items.isEmpty();
    }

    public int getItemCount() {
        return items.size();
    }
}
