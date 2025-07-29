package pl.polardev.scase.listener;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.polardev.scase.CasePlugin;
import pl.polardev.scase.helper.ChatHelper;
import pl.polardev.scase.inventory.CrateMainInventory;
import pl.polardev.scase.model.Crate;

public class CrateListener implements Listener {
    private final CasePlugin plugin;

    public CrateListener(CasePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();

        // Rate limiting to prevent spam
        if (!plugin.getRateLimitManager().canInteract(player)) {
            return;
        }

        String crateName = plugin.getCrateManager().getCrateNameFromBlock(block);
        if (crateName == null) return;

        event.setCancelled(true);

        // Additional GUI opening rate limit
        if (!plugin.getRateLimitManager().canOpenGUI(player)) {
            return;
        }

        Crate crate = plugin.getCrateManager().getCrate(crateName);
        if (crate != null) {
            new CrateMainInventory(plugin, player, crate).open();
        } else {
            ChatHelper.showTitle(player, "<red>Błąd", "<gray>Skrzynka nie została znaleziona");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        String crateName = plugin.getCrateManager().getCrateNameFromBlock(block);

        if (crateName != null) {
            Player player = event.getPlayer();

            if (!player.hasPermission("simplecase.admin")) {
                event.setCancelled(true);
                ChatHelper.showTitle(player, "<red>Brak Uprawnień", "<gray>Nie możesz zniszczyć tej skrzynki");
            } else {
                ChatHelper.showTitle(player, "<yellow>Uwaga", "<gray>Zniszczono skrzynkę: <gold>" + crateName);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up player data when they leave
        plugin.getCrateManager().invalidatePlayerCache(event.getPlayer());
        plugin.getAnimationManager().stopAnimation(event.getPlayer());
    }
}
