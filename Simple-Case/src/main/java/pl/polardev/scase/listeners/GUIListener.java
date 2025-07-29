package pl.polardev.scase.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;
import pl.polardev.scase.CasePlugin;
import pl.polardev.scase.helpers.GUIHelper;

public class GUIListener implements Listener {
    private final CasePlugin plugin;

    public GUIListener(CasePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        final InventoryHolder holder = event.getInventory().getHolder();
        if (!GUIHelper.isPluginGUI(holder)) return;

        final boolean isTopInventory = event.getRawSlot() < event.getInventory().getSize();

        if (GUIHelper.shouldCancelClick(holder)) {
            event.setCancelled(true);
        }

        GUIHelper.handleGUIClick(holder, event.getSlot(), event.getCurrentItem(), isTopInventory);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        final InventoryHolder holder = event.getInventory().getHolder();
        if (!GUIHelper.requiresCloseHandling(holder)) return;

        GUIHelper.handleGUIClose(holder);
    }
}
