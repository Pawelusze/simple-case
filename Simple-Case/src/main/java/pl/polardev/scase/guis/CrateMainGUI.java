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

public class CrateMainGUI implements InventoryHolder {
    private final CasePlugin plugin;
    private final Player player;
    private final Crate crate;
    private final Inventory inventory;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private static final int NORMAL_OPEN_SLOT_1 = 46;
    private static final int NORMAL_OPEN_SLOT_2 = 47;
    private static final int NORMAL_OPEN_SLOT_3 = 48;
    private static final int CLOSE_SLOT = 49;
    private static final int ANIMATION_OPEN_SLOT_1 = 50;
    private static final int ANIMATION_OPEN_SLOT_2 = 51;
    private static final int ANIMATION_OPEN_SLOT_3 = 52;

    public CrateMainGUI(CasePlugin plugin, Player player, Crate crate) {
        this.plugin = plugin;
        this.player = player;
        this.crate = crate;
        this.inventory = Bukkit.createInventory(this, 54, mm.deserialize("<gold>Skrzynka: " + crate.getName()));
        setupInventory();
    }

    private void setupInventory() {
        List<ItemStack> items = crate.getItems();

        for (int i = 0; i < 45 && i < items.size(); i++) {
            inventory.setItem(i, items.get(i));
        }

        boolean hasKey = hasValidKey();

        ItemStack normalOpen = createButton(Material.LIME_DYE, "<green>Otwórz Skrzynkę",
            "<gray>Kliknij aby otworzyć tę skrzynkę",
            hasKey ? "<green>✓ Masz odpowiedni klucz!" : "<red>✗ Brak odpowiedniego klucza");
        inventory.setItem(NORMAL_OPEN_SLOT_1, normalOpen);
        inventory.setItem(NORMAL_OPEN_SLOT_2, normalOpen);
        inventory.setItem(NORMAL_OPEN_SLOT_3, normalOpen);

        inventory.setItem(CLOSE_SLOT, createButton(Material.BARRIER, "<red>Zamknij",
            "<gray>Kliknij aby zamknąć to gui"));

        ItemStack animationOpen = createButton(Material.MAGENTA_DYE, "<light_purple>Otwórz z Animacją",
            "<gray>Spektakularne otwarcie skrzynki",
            "<gold>✨ Animacja jak w CS:GO!",
            "<gray>Piękna roulette z 7-sekundową animacją",
            hasKey ? "<green>✓ Masz odpowiedni klucz!" : "<red>✗ Brak odpowiedniego klucza");
        inventory.setItem(ANIMATION_OPEN_SLOT_1, animationOpen);
        inventory.setItem(ANIMATION_OPEN_SLOT_2, animationOpen);
        inventory.setItem(ANIMATION_OPEN_SLOT_3, animationOpen);
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

    public void open() {
        player.openInventory(inventory);
    }

    public void handleClick(ItemStack clickedItem) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        Material material = clickedItem.getType();

        if (material == Material.LIME_DYE) {
            if (!hasValidKey()) {
                showTitle("<red>Brak Klucza", "<gray>Potrzebujesz odpowiedniego klucza aby otworzyć tę skrzynkę");
                return;
            }
            new CrateOpenGUI(plugin, player, crate).open();
        } else if (material == Material.MAGENTA_DYE) {
            if (!hasValidKey()) {
                showTitle("<red>Brak Klucza", "<gray>Potrzebujesz odpowiedniego klucza aby otworzyć tę skrzynkę");
                return;
            }
            new CrateAnimationGUI(plugin, player, crate).open();
        } else if (material == Material.BARRIER) {
            player.closeInventory();
        }
    }

    private boolean hasValidKey() {
        ItemStack requiredKey = crate.getKeyItem();
        if (requiredKey == null) return true;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(requiredKey)) {
                return true;
            }
        }
        return false;
    }

    private void showTitle(String title, String subtitle) {
        player.showTitle(Title.title(
            mm.deserialize(title),
            mm.deserialize(subtitle),
            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(2), Duration.ofMillis(500))
        ));
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        return inventory;
    }
}
