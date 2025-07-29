package pl.polardev.scase.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;
import pl.polardev.scase.CasePlugin;
import pl.polardev.scase.guis.CrateAnimationGUI;
import pl.polardev.scase.guis.CrateEditGUI;
import pl.polardev.scase.guis.CrateMainGUI;
import pl.polardev.scase.guis.CrateOpenGUI;

public class GUIListener implements Listener {
    private final CasePlugin plugin;

    public GUIListener(CasePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof CrateMainGUI gui) {
            event.setCancelled(true);
            if (event.getRawSlot() < event.getInventory().getSize()) {
                gui.handleClick(event.getCurrentItem());
            }
        } else if (holder instanceof CrateOpenGUI gui) {
            event.setCancelled(true);
            if (event.getRawSlot() < event.getInventory().getSize()) {
                gui.handleClick(event.getRawSlot());
            }
        } else if (holder instanceof CrateAnimationGUI gui) {
            event.setCancelled(true);
            gui.handleClick(event.getRawSlot());
        } else if (holder instanceof CrateEditGUI gui) {
            int slot = event.getRawSlot();

            if (slot >= 0 && slot < 54) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (!player.getOpenInventory().getTopInventory().equals(gui.getInventory())) return;

                    var item = gui.getInventory().getItem(slot);
                    if (item == null || item.getType().isAir()) {
                        gui.handleItemRemove(slot);
                    } else {
                        gui.handleItemPlace(slot, item);
                    }
                }, 1L);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof CrateEditGUI gui) {
            gui.autoSave();
        } else if (holder instanceof CrateAnimationGUI gui) {
            gui.onClose();
        }
    }
}
