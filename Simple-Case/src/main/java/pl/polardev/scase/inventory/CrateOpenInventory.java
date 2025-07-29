package pl.polardev.scase.inventory;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import pl.polardev.scase.CasePlugin;
import pl.polardev.scase.helper.ChatHelper;
import pl.polardev.scase.helper.ItemBuilder;
import pl.polardev.scase.model.Crate;

public class CrateOpenInventory implements InventoryHolder {
    private final CasePlugin plugin;
    private final Player player;
    private final Crate crate;
    private final Inventory inventory;
    private final ItemStack wonItem;

    private static final int ITEM_SLOT = 13;
    private static final int NEXT_SLOT = 15;
    private static final int CLOSE_SLOT = 14;

    public CrateOpenInventory(CasePlugin plugin, Player player, Crate crate) {
        this.plugin = plugin;
        this.player = player;
        this.crate = crate;
        this.wonItem = crate.getRandomItem();
        this.inventory = Bukkit.createInventory(this, 27, "Wynik: " + crate.getName());

        setupInventory();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private void setupInventory() {
        // Clear inventory first
        inventory.clear();

        // Show won item in center
        if (wonItem != null) {
            inventory.setItem(ITEM_SLOT, wonItem.clone());
        }

        ItemStack nextItem = ItemBuilder.of(Material.LIME_DYE)
                .name("<green>Następny Przedmiot")
                .lore("<gray>Kliknij aby otworzyć ponownie")
                .build();
        inventory.setItem(NEXT_SLOT, nextItem);

        ItemStack closeItem = ItemBuilder.of(Material.RED_DYE)
                .name("<red>Zamknij")
                .lore("<gray>Kliknij aby zamknąć te GUI")
                .build();
        inventory.setItem(CLOSE_SLOT, closeItem);
    }

    public void open() {
        player.openInventory(inventory);

        if (wonItem != null) {
            ItemStack clonedItem = wonItem.clone();
            player.getInventory().addItem(clonedItem).values()
                    .forEach(excess -> player.getWorld().dropItem(player.getLocation(), excess));
        }
    }

    public void handleClick(int slot) {
        if (slot == NEXT_SLOT) {
            if (hasPhysicalKey(player, crate)) {
                removePhysicalKey(player, crate);
                new CrateOpenInventory(plugin, player, crate).open();
            } else {
                ChatHelper.sendMessage(player, "<red>Nie posiadasz klucza do tej skrzynki!");
            }
        } else if (slot == CLOSE_SLOT) {
            player.closeInventory();
        }
    }

    private boolean hasPhysicalKey(Player player, Crate crate) {
        ItemStack keyItem = crate.getKeyItem();
        if (keyItem == null) return true; // No key required

        return player.getInventory().containsAtLeast(keyItem, 1);
    }

    private void removePhysicalKey(Player player, Crate crate) {
        ItemStack keyItem = crate.getKeyItem();
        if (keyItem == null) return; // No key required

        player.getInventory().removeItem(keyItem.clone());
    }

    public boolean isThisInventory(Inventory inventory) {
        return this.inventory.equals(inventory);
    }

    public ItemStack getWonItem() {
        return wonItem != null ? wonItem.clone() : null;
    }

    public Crate getCrate() {
        return crate;
    }
}
