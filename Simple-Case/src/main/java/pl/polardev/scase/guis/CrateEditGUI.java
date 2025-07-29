package pl.polardev.scase.guis;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import pl.polardev.scase.CasePlugin;
import pl.polardev.scase.helpers.ChatHelper;
import pl.polardev.scase.models.Crate;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class CrateEditGUI implements InventoryHolder {
    private final CasePlugin plugin;
    private final Player player;
    private final Crate crate;
    private final Inventory inventory;

    private static final int MAX_SLOTS = 54;

    public CrateEditGUI(CasePlugin plugin, Player player, Crate crate) {
        this.plugin = plugin;
        this.player = player;
        this.crate = crate;
        this.inventory = Bukkit.createInventory(this, MAX_SLOTS, ChatHelper.deserialize("<gold>Edycja: " + crate.getName()));
        setupInventory();
    }

    private void setupInventory() {
        final List<ItemStack> items = crate.getItems();

        for (int i = 0; i < Math.min(MAX_SLOTS, items.size()); i++) {
            inventory.setItem(i, items.get(i));
        }
    }

    public void open() {
        player.openInventory(inventory);
    }

    public void onClose() {
        saveChanges();
        ChatHelper.showTitle(player, "<green>Zapisano", "<gray>Zmiany zostały automatycznie zapisane");
    }

    private void saveChanges() {
        final List<ItemStack> validItems = Arrays.stream(inventory.getContents())
                .filter(item -> item != null && !item.getType().isAir())
                .toList();

        crate.setItems(validItems);
        plugin.getCrateManager().saveCrateImmediately(crate);
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        return inventory;
    }
}
