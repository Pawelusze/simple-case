package pl.polardev.scase.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;
import pl.polardev.scase.CasePlugin;
import pl.polardev.scase.inventory.*;

public class GUIListener implements Listener {
    private final CasePlugin plugin;

    public GUIListener(CasePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        InventoryHolder holder = event.getInventory().getHolder();
        if (holder == null) return;

        switch (holder) {
            case CrateMainInventory mainGui -> {
                event.setCancelled(true);
                mainGui.handleClick(event.getSlot(), event.getCurrentItem());
            }
            case CrateOpenInventory openGui -> {
                event.setCancelled(true);
                openGui.handleClick(event.getSlot());
            }
            case CrateAnimationInventory animGui -> {
                event.setCancelled(true);
                animGui.handleClick(event.getSlot());
            }
            case CrateEditInventory editGui -> {
                // Allow ALL interactions in edit GUI - no cancellation at all
                // Players can freely drag, drop, add, remove items
            }
            default -> {
                // Not our GUI
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        InventoryHolder holder = event.getInventory().getHolder();
        if (holder == null) return;

        switch (holder) {
            case CrateEditInventory editGui -> editGui.onClose();
            case CrateAnimationInventory animGui -> animGui.onClose();
            default -> {
                // No special close handling needed
            }
        }
    }
}
