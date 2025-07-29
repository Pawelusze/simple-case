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

public class CrateAnimationInventory implements InventoryHolder {
    private final CasePlugin plugin;
    private final Player player;
    private final Crate crate;
    private final Inventory inventory;
    private boolean animationRunning;

    private static final int NEXT_BUTTON_SLOT = 23;
    private static final int CLOSE_BUTTON_SLOT = 21;

    public CrateAnimationInventory(CasePlugin plugin, Player player, Crate crate) {
        this.plugin = plugin;
        this.player = player;
        this.crate = crate;
        this.inventory = Bukkit.createInventory(this, 27, "Otwieranie: " + crate.getName());
        this.animationRunning = false;

        setupInitialInventory();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private void setupInitialInventory() {
        // Pre-fill with random items for initial display
        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        for (int slot : slots) {
            ItemStack randomItem = crate.getRandomItem();
            if (randomItem != null) {
                inventory.setItem(slot, randomItem);
            }
        }
    }

    public void open() {
        player.openInventory(inventory);
        startAnimation();
    }

    private void startAnimation() {
        animationRunning = true;
        plugin.getAnimationManager().startAnimation(player, this, crate);
    }

    public void finishAnimation(ItemStack winningItem) {
        animationRunning = false;

        if (winningItem != null) {
            player.getInventory().addItem(winningItem.clone()).values()
                .forEach(excess -> player.getWorld().dropItem(player.getLocation(), excess));
        }

        addControlButtons();
    }

    private void addControlButtons() {
        ItemStack nextItem = ItemBuilder.of(Material.LIME_DYE)
            .name("<green>Otwórz Ponownie")
            .lore("<gray>Kliknij aby otworzyć ponownie")
            .build();
        inventory.setItem(NEXT_BUTTON_SLOT, nextItem);

        ItemStack closeItem = ItemBuilder.of(Material.RED_DYE)
            .name("<red>Zamknij")
            .lore("<gray>Kliknij aby zamknąć GUI")
            .build();
        inventory.setItem(CLOSE_BUTTON_SLOT, closeItem);
    }

    public void handleClick(int slot) {
        if (animationRunning) return;

        switch (slot) {
            case NEXT_BUTTON_SLOT -> {
                if (hasPhysicalKey(player, crate)) {
                    removePhysicalKey(player, crate);
                    new CrateAnimationInventory(plugin, player, crate).open();
                } else {
                    ChatHelper.sendMessage(player, "<red>Nie posiadasz klucza do tej skrzynki!");
                }
            }
            case CLOSE_BUTTON_SLOT -> player.closeInventory();
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

    public void onClose() {
        plugin.getAnimationManager().stopAnimation(player);
    }

    public Crate getCrate() {
        return crate;
    }
}
