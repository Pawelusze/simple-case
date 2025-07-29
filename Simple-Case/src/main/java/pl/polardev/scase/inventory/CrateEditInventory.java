package pl.polardev.scase.inventory;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import pl.polardev.scase.CasePlugin;
import pl.polardev.scase.helper.ChatHelper;
import pl.polardev.scase.model.Crate;

import java.util.ArrayList;
import java.util.List;

public class CrateEditInventory implements InventoryHolder {
    private final CasePlugin plugin;
    private final Player player;
    private final Crate crate;
    private final Inventory inventory;

    public CrateEditInventory(CasePlugin plugin, Player player, Crate crate) {
        this.plugin = plugin;
        this.player = player;
        this.crate = crate;
        this.inventory = Bukkit.createInventory(this, 54, "Edytuj: " + crate.getName());

        setupInventory();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private void setupInventory() {
        List<ItemStack> items = crate.getItems();
        for (int i = 0; i < Math.min(items.size(), 54); i++) {
            inventory.setItem(i, items.get(i));
        }
    }

    public void open() {
        player.openInventory(inventory);
    }

    public void onClose() {
        List<ItemStack> newItems = new ArrayList<>();

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                newItems.add(item.clone());
            }
        }

        crate.setItems(newItems);
        plugin.getCrateManager().saveCrate(crate);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            ChatHelper.sendMessage(player, "<green>Zapisano zawartość skrzynki " + crate.getName());
        }, 1L);
    }

    public Crate getCrate() {
        return crate;
    }
}
