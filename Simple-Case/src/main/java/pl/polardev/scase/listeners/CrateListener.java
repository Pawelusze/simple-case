package pl.polardev.scase.listeners;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import pl.polardev.scase.CasePlugin;
import pl.polardev.scase.guis.CrateMainGUI;
import pl.polardev.scase.helpers.ChatHelper;
import pl.polardev.scase.models.Crate;

public class CrateListener implements Listener {
    private final CasePlugin plugin;
    private final NamespacedKey crateKey;

    public CrateListener(CasePlugin plugin) {
        this.plugin = plugin;
        this.crateKey = new NamespacedKey(plugin, "crate_name");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof TileState tileState)) return;

        PersistentDataContainer container = tileState.getPersistentDataContainer();
        String crateName = container.get(crateKey, PersistentDataType.STRING);

        if (crateName != null) {
            event.setCancelled(true);

            Crate crate = plugin.getCrateManager().getCrate(crateName);
            if (crate != null) {
                new CrateMainGUI(plugin, event.getPlayer(), crate).open();
            } else {
                ChatHelper.showTitle(event.getPlayer(), "<red>Błąd", "<gray>Skrzynka nie została znaleziona");
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!(block.getState() instanceof TileState tileState)) return;

        PersistentDataContainer container = tileState.getPersistentDataContainer();
        String crateName = container.get(crateKey, PersistentDataType.STRING);

        if (crateName != null) {
            Player player = event.getPlayer();
            if (!player.hasPermission("simplecase.admin")) {
                event.setCancelled(true);
                ChatHelper.showTitle(player, "<red>Chroniona", "<gray>Ta skrzynka jest chroniona");
            }
        }
    }
}
