package pl.polardev.scase.guis;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.polardev.scase.CasePlugin;
import pl.polardev.scase.models.Crate;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public class CrateOpenGUI implements InventoryHolder {
    private final CasePlugin plugin;
    private final Player player;
    private final Crate crate;
    private final Inventory inventory;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private ItemStack currentItem;

    public CrateOpenGUI(CasePlugin plugin, Player player, Crate crate) {
        this.plugin = plugin;
        this.player = player;
        this.crate = crate;
        this.inventory = Bukkit.createInventory(this, 27, mm.deserialize("<gold>Otwieranie: " + crate.getName()));
        setupInventory();
        rollNewItem();
    }

    private void setupInventory() {
        inventory.setItem(13, createButton(Material.LIME_DYE, "<green>Otwórz Ponownie",
            "<gray>Kliknij aby otworzyć ponownie"));

        inventory.setItem(14, createButton(Material.BARRIER, "<red>Zamknij",
            "<gray>Kliknij aby zamknąć to gui"));
    }

    private ItemStack createButton(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mm.deserialize(name));
        if (lore.length > 0) {
            List<Component> loreList = new java.util.ArrayList<>();
            for (String line : lore) {
                loreList.add(mm.deserialize(line));
            }
            meta.lore(loreList);
        }
        item.setItemMeta(meta);
        return item;
    }

    private void rollNewItem() {
        currentItem = crate.getRandomItem();
        if (currentItem != null) {
            inventory.setItem(11, currentItem);
            showTitle("<green>Wylosowano!", "<gray>Kliknij przycisk aby odebrać przedmiot");
        } else {
            inventory.setItem(11, createButton(Material.BARRIER, "<red>Brak Przedmiotów",
                "<gray>Ta skrzynka jest pusta"));
        }
    }

    public void open() {
        if (!consumeKey()) {
            player.sendMessage(mm.deserialize("<red>Brak klucza! Potrzebujesz odpowiedniego klucza aby otworzyć tę skrzynkę."));
            return;
        }
        player.openInventory(inventory);
    }

    public void handleClick(int slot) {
        if (slot == 13) {
            if (!consumeKey()) {
                showTitle("<red>Brak Klucza", "<gray>Potrzebujesz klucza aby wylosować ponownie");
                return;
            }

            if (currentItem != null) {
                addItemToInventory(currentItem);
            }

            rollNewItem();
            showTitle("<green>Wylosowano", "<gray>Nowy przedmiot! Kliknij 'Zamknij' aby odebrać");
        } else if (slot == 14) {
            if (currentItem != null) {
                addItemToInventory(currentItem);
                showTitle("<green>Otrzymano", "<gray>Przedmiot dodany do ekwipunku");
            }
            player.closeInventory();
        }
    }

    private boolean consumeKey() {
        ItemStack requiredKey = crate.getKeyItem();
        if (requiredKey == null) return true; // Brak wymaganego klucza

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.isSimilar(requiredKey)) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().setItem(i, null);
                }
                return true;
            }
        }
        return false;
    }

    private void addItemToInventory(ItemStack item) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
        if (!leftover.isEmpty()) {
            for (ItemStack leftoverItem : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftoverItem);
            }
        }
    }

    private void showTitle(String title, String subtitle) {
        player.showTitle(Title.title(
            mm.deserialize(title),
            mm.deserialize(subtitle),
            Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(2), Duration.ofMillis(300))
        ));
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        return inventory;
    }
}
