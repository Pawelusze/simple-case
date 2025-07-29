package pl.polardev.scase.guis;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import pl.polardev.scase.CasePlugin;
import pl.polardev.scase.helpers.ChatHelper;
import pl.polardev.scase.helpers.ItemBuilder;
import pl.polardev.scase.models.Crate;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class CrateMainGUI implements InventoryHolder {
    private final CasePlugin plugin;
    private final Player player;
    private final Crate crate;
    private final Inventory inventory;

    private static final int PREVIEW_SLOTS_END = 44;
    private static final Set<Integer> NORMAL_OPEN_SLOTS = Set.of(46, 47, 48);
    private static final int CLOSE_SLOT = 49;
    private static final Set<Integer> ANIMATION_OPEN_SLOTS = Set.of(50, 51, 52);

    public CrateMainGUI(CasePlugin plugin, Player player, Crate crate) {
        this.plugin = plugin;
        this.player = player;
        this.crate = crate;
        this.inventory = Bukkit.createInventory(this, 54, ChatHelper.deserialize("<gold>Skrzynka: " + crate.getName()));
        setupInventory();
    }

    private void setupInventory() {
        populatePreviewSlots();
        setupActionButtons();
    }

    private void populatePreviewSlots() {
        final List<ItemStack> items = crate.getItems();
        
        for (int i = 0; i <= PREVIEW_SLOTS_END && i < items.size(); i++) {
            inventory.setItem(i, items.get(i));
        }
    }

    private void setupActionButtons() {
        final boolean hasKey = hasValidKey();
        
        final ItemStack normalOpen = createNormalOpenButton(hasKey);
        NORMAL_OPEN_SLOTS.forEach(slot -> inventory.setItem(slot, normalOpen));

        inventory.setItem(CLOSE_SLOT, createCloseButton());

        final ItemStack animationOpen = createAnimationOpenButton(hasKey);
        ANIMATION_OPEN_SLOTS.forEach(slot -> inventory.setItem(slot, animationOpen));
    }

    private ItemStack createNormalOpenButton(boolean hasKey) {
        return ItemBuilder.of(Material.LIME_DYE)
            .name("<green>Otwórz Skrzynkę")
            .lore(
                "<gray>Kliknij aby otworzyć tę skrzynkę",
                hasKey ? "<green>✓ Masz odpowiedni klucz!" : "<red>✗ Brak odpowiedniego klucza"
            )
            .glow()
            .build();
    }

    private ItemStack createCloseButton() {
        return ItemBuilder.of(Material.BARRIER)
            .name("<red>Zamknij")
            .lore("<gray>Kliknij aby zamknąć to gui")
            .build();
    }

    private ItemStack createAnimationOpenButton(boolean hasKey) {
        return ItemBuilder.of(Material.MAGENTA_DYE)
            .name("<light_purple>Otwórz z Animacją")
            .lore(
                "<gray>Spektakularne otwarcie skrzynki",
                "<gold>✨ Animacja jak w CS:GO!",
                "<gray>Piękna roulette z 7-sekundową animacją",
                hasKey ? "<green>✓ Masz odpowiedni klucz!" : "<red>✗ Brak odpowiedniego klucza"
            )
            .glow()
            .build();
    }

    public void open() {
        player.openInventory(inventory);
    }

    public void handleClick(int slot, ItemStack clickedItem) {
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        switch (slot) {
            case 46, 47, 48 -> handleNormalOpen();
            case 50, 51, 52 -> handleAnimationOpen();
            case 49 -> player.closeInventory();
        }
    }

    private void handleNormalOpen() {
        if (!hasValidKey()) {
            ChatHelper.showTitle(player, "<red>Brak Klucza", "<gray>Potrzebujesz odpowiedniego klucza aby otworzyć tę skrzynkę");
            return;
        }
        player.closeInventory();
        new CrateOpenGUI(plugin, player, crate).open();
    }

    private void handleAnimationOpen() {
        if (!hasValidKey()) {
            ChatHelper.showTitle(player, "<red>Brak Klucza", "<gray>Potrzebujesz odpowiedniego klucza aby otworzyć tę skrzynkę");
            return;
        }
        player.closeInventory();
        new CrateAnimationGUI(plugin, player, crate).open();
    }

    private boolean hasValidKey() {
        return crate.getKeyItem()
            .map(requiredKey ->
                Arrays.stream(player.getInventory().getContents())
                    .anyMatch(item -> item != null && item.isSimilar(requiredKey))
            )
            .orElse(true);
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        return inventory;
    }
}
