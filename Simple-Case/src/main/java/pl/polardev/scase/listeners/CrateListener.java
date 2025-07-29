package pl.polardev.scase.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import pl.polardev.scase.CasePlugin;
import pl.polardev.scase.guis.CrateMainGUI;
import pl.polardev.scase.models.Crate;

import java.time.Duration;

public class CrateListener implements Listener {
    private final CasePlugin plugin;
    private final NamespacedKey crateKey;

    public CrateListener(CasePlugin plugin) {
        this.plugin = plugin;
        this.crateKey = new NamespacedKey(plugin, "crate_name");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!(block.getState() instanceof TileState tileState)) return;

        PersistentDataContainer container = tileState.getPersistentDataContainer();
        String crateName = container.get(crateKey, PersistentDataType.STRING);

        if (crateName != null) {
            Player player = event.getPlayer();
            if (!player.isOp()) {
                event.setCancelled(true);
                showTitle(player, "Chroniona", "Ta skrzynka jest chroniona");
                return;
            }

            Crate crate = plugin.getCrateManager().getCrate(crateName);
            if (crate != null) {
                ItemStack crateItem = crate.getDisplayItem();
                ItemMeta meta = crateItem.getItemMeta();
                PersistentDataContainer itemContainer = meta.getPersistentDataContainer();
                itemContainer.set(crateKey, PersistentDataType.STRING, crateName);
                crateItem.setItemMeta(meta);

                block.getWorld().dropItemNaturally(block.getLocation(), crateItem);
                event.setDropItems(false);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (!event.getAction().name().contains("RIGHT_CLICK")) return;

        Block block = event.getClickedBlock();
        if (!(block.getState() instanceof TileState tileState)) return;

        PersistentDataContainer container = tileState.getPersistentDataContainer();
        String crateName = container.get(crateKey, PersistentDataType.STRING);

        if (crateName != null) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            Crate crate = plugin.getCrateManager().getCrate(crateName);

            if (crate == null) {
                showTitle(player, "§cBłąd", "§7Dane skrzynki nie zostały znalezione");
                return;
            }

            new CrateMainGUI(plugin, player, crate).open();
        }
    }

    private void showTitle(Player player, String title, String subtitle) {
        player.showTitle(Title.title(
            Component.text(title),
            Component.text(subtitle),
            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(2), Duration.ofMillis(500))
        ));
    }
}
