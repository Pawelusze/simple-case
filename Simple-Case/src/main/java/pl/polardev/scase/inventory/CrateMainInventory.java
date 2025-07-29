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

import java.util.Set;

public class CrateMainInventory implements InventoryHolder {
    private final CasePlugin plugin;
    private final Player player;
    private final Crate crate;
    private final Inventory inventory;

    private static final Set<Integer> NORMAL_OPEN_SLOTS = Set.of(46, 47, 48);
    private static final Set<Integer> ANIMATION_OPEN_SLOTS = Set.of(50, 51, 52);
    private static final int CLOSE_SLOT = 49;

    public CrateMainInventory(CasePlugin plugin, Player player, Crate crate) {
        this.plugin = plugin;
        this.player = player;
        this.crate = crate;
        this.inventory = Bukkit.createInventory(this, 54, "Skrzynka: " + crate.getName());

        setupInventory();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private void setupInventory() {
        ItemStack normalOpen = ItemBuilder.of(Material.CHEST)
                .name("<green>Otwórz Normalnie")
                .lore("<gray>Kliknij aby otworzyć skrzynkę", "<gray>bez animacji")
                .build();

        for (int slot : NORMAL_OPEN_SLOTS) {
            inventory.setItem(slot, normalOpen);
        }

        ItemStack animationOpen = ItemBuilder.of(Material.LIME_DYE)
                .name("<light_purple>Otwórz z Animacją")
                .lore("<gray>Kliknij aby otworzyć skrzynkę", "<gray>z animacją ruletki")
                .build();

        for (int slot : ANIMATION_OPEN_SLOTS) {
            inventory.setItem(slot, animationOpen);
        }

        ItemStack closeItem = ItemBuilder.of(Material.BARRIER)
                .name("<red>Zamknij")
                .lore("<gray>Kliknij aby zamknąć GUI")
                .build();

        inventory.setItem(CLOSE_SLOT, closeItem);
    }

    public void open() {
        player.openInventory(inventory);
    }

    public void handleClick(int slot, ItemStack clickedItem) {
        if (NORMAL_OPEN_SLOTS.contains(slot)) {
            handleNormalOpen();
        } else if (ANIMATION_OPEN_SLOTS.contains(slot)) {
            handleAnimationOpen();
        } else if (slot == CLOSE_SLOT) {
            player.closeInventory();
        }
    }

    private void handleNormalOpen() {
        if (hasPhysicalKey(player, crate)) {
            removePhysicalKey(player, crate);
            new CrateOpenInventory(plugin, player, crate).open();
        } else {
            ChatHelper.sendMessage(player, "<red>Nie posiadasz klucza do tej skrzynki!");
        }
    }

    private void handleAnimationOpen() {
        if (hasPhysicalKey(player, crate)) {
            removePhysicalKey(player, crate);
            new CrateAnimationInventory(plugin, player, crate).open();
        } else {
            ChatHelper.sendMessage(player, "<red>Nie posiadasz klucza do tej skrzynki!");
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

    public Crate getCrate() {
        return crate;
    }
}
