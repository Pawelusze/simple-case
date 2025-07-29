package pl.polardev.scase.models;

import org.bukkit.inventory.ItemStack;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Crate {
    private final String name;
    private ItemStack displayItem;
    private ItemStack keyItem;
    private final List<ItemStack> items;

    public Crate(String name, ItemStack displayItem) {
        this.name = Objects.requireNonNull(name, "Crate name cannot be null");
        this.displayItem = Objects.requireNonNull(displayItem, "Display item cannot be null").clone();
        this.items = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public ItemStack getDisplayItem() {
        return displayItem.clone();
    }

    public void setDisplayItem(ItemStack displayItem) {
        this.displayItem = Objects.requireNonNull(displayItem, "Display item cannot be null").clone();
    }

    public Optional<ItemStack> getKeyItem() {
        return Optional.ofNullable(keyItem).map(ItemStack::clone);
    }

    public void setKeyItem(ItemStack keyItem) {
        this.keyItem = keyItem != null ? keyItem.clone() : null;
    }

    public List<ItemStack> getItems() {
        return items.stream()
                .map(ItemStack::clone)
                .toList();
    }

    public void addItem(ItemStack item) {
        if (item != null && !item.getType().isAir()) {
            items.add(item.clone());
        }
    }

    public boolean removeItem(int index) {
        if (isValidIndex(index)) {
            items.remove(index);
            return true;
        }
        return false;
    }

    public void setItems(Collection<ItemStack> items) {
        this.items.clear();
        items.stream()
                .filter(Objects::nonNull)
                .filter(item -> !item.getType().isAir())
                .map(ItemStack::clone)
                .forEach(this.items::add);
    }

    public boolean hasKey() {
        return keyItem != null;
    }

    public Optional<ItemStack> getRandomItem() {
        return items.isEmpty()
            ? Optional.empty()
            : Optional.of(items.get(ThreadLocalRandom.current().nextInt(items.size())).clone());
    }

    public int getItemCount() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void clearItems() {
        items.clear();
    }

    private boolean isValidIndex(int index) {
        return index >= 0 && index < items.size();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Crate crate = (Crate) obj;
        return Objects.equals(name.toLowerCase(), crate.name.toLowerCase());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.toLowerCase());
    }

    @Override
    public String toString() {
        return "Crate{name='%s', itemCount=%d, hasKey=%b}".formatted(name, items.size(), hasKey());
    }
}
