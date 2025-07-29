package pl.polardev.scase.guis;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.polardev.scase.CasePlugin;
import pl.polardev.scase.models.Crate;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

public class CrateEditGUI implements InventoryHolder {
    private final CasePlugin plugin;
    private final Player player;
    private final Crate crate;
    private final Inventory inventory;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private static final String[] COLORS = {"<red>", "<gold>", "<yellow>", "<green>", "<aqua>", "<light_purple>", "<blue>", "<dark_purple>"};

    public CrateEditGUI(CasePlugin plugin, Player player, Crate crate) {
        this.plugin = plugin;
        this.player = player;
        this.crate = crate;
        this.inventory = Bukkit.createInventory(this, 54, mm.deserialize("<gold>Edycja: " + crate.getName()));
        setupInventory();
    }

    private void setupInventory() {
        List<ItemStack> items = crate.getItems();

        int size = Math.min(54, items.size());
        for (int i = 0; i < size; i++) {
            ItemStack item = items.get(i).clone();
            addColorGlow(item, i);
            inventory.setItem(i, item);
        }
    }

    private void addColorGlow(ItemStack item, int slot) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<Component> lore = meta.lore();
        if (lore == null) lore = List.of();

        String color = COLORS[slot % COLORS.length];

        List<Component> newLore = new java.util.ArrayList<>(lore);
        newLore.add(0, mm.deserialize(color + "✦ Slot " + (slot + 1) + " ✦"));
        meta.lore(newLore);
        item.setItemMeta(meta);
    }

    public void open() {
        player.openInventory(inventory);
    }

    public void handleItemPlace(int slot, ItemStack item) {
        if (slot >= 0 && slot < 54 && item != null && item.getType() != Material.AIR) {
            ItemStack newItem = item.clone();
            addColorGlow(newItem, slot);
            inventory.setItem(slot, newItem);
        }
    }

    public void handleItemRemove(int slot) {
        if (slot >= 0 && slot < 54) {
            inventory.setItem(slot, null);
        }
    }

    public void autoSave() {
        saveChanges();
        showTitle("<green>Zapisano", "<gray>Zmiany zostały automatycznie zapisane");
    }

    private void saveChanges() {
        crate.getItems().clear();

        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < 54; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() != Material.AIR) {
                ItemStack cleanItem = item.clone();
                ItemMeta meta = cleanItem.getItemMeta();
                if (meta != null) {
                    List<Component> lore = meta.lore();
                    if (lore != null && !lore.isEmpty()) {
                        List<Component> newLore = new java.util.ArrayList<>(lore);
                        if (!newLore.isEmpty() && newLore.get(0).toString().contains("✦ Slot")) {
                            newLore.remove(0);
                            meta.lore(newLore.isEmpty() ? null : newLore);
                            cleanItem.setItemMeta(meta);
                        }
                    }
                }
                crate.addItem(cleanItem);
            }
        }

        plugin.getCrateManager().markForSave(crate.getName());
    }

    private void showTitle(String title, String subtitle) {
        player.showTitle(Title.title(
            mm.deserialize(title),
            mm.deserialize(subtitle),
            Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(1), Duration.ofMillis(300))
        ));
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        return inventory;
    }
}
