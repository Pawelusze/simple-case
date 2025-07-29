package pl.polardev.scase.guis;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import pl.polardev.scase.CasePlugin;
import pl.polardev.scase.helpers.ChatHelper;
import pl.polardev.scase.helpers.ItemBuilder;
import pl.polardev.scase.models.Crate;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class CrateOpenGUI implements InventoryHolder {
    private final CasePlugin plugin;
    private final Player player;
    private final Crate crate;
    private final Inventory inventory;

    private Optional<ItemStack> currentItem = Optional.empty();
    private boolean isProcessing = false;

    private static final int ITEM_DISPLAY_SLOT = 4;
    private static final int REOPEN_SLOT = 13;
    private static final int CLOSE_SLOT = 14;
    private static final int ANIMATION_DELAY_TICKS = 10;

    public CrateOpenGUI(CasePlugin plugin, Player player, Crate crate) {
        this.plugin = plugin;
        this.player = player;
        this.crate = crate;
        this.inventory = Bukkit.createInventory(this, 27,
            ChatHelper.deserialize("<gold>Otwieranie: " + crate.getName()));

        initializeGUI();
    }

    private void initializeGUI() {
        setupInventory();
        performInitialRoll();
    }

    private void setupInventory() {
        final boolean hasKey = hasValidKey();

        inventory.setItem(REOPEN_SLOT, createReopenButton(hasKey));
        inventory.setItem(CLOSE_SLOT, createCloseButton());
    }

    private ItemStack createReopenButton(boolean hasKey) {
        return ItemBuilder.of(Material.LIME_DYE)
            .name("<green>Otwórz Ponownie")
            .lore(
                "<gray>Kliknij aby otworzyć ponownie",
                hasKey ? "<green>✓ Masz klucz!" : "<red>✗ Brak klucza",
                "<yellow>Koszt: 1 klucz"
            )
            .glow()
            .build();
    }

    private ItemStack createCloseButton() {
        return ItemBuilder.of(Material.BARRIER)
            .name("<red>Zamknij")
            .lore(
                "<gray>Kliknij aby zamknąć to gui",
                "<yellow>Zabierz swój przedmiot!"
            )
            .build();
    }

    private void performInitialRoll() {
        rollNewItemWithAnimation();
    }

    private void rollNewItemWithAnimation() {
        if (isProcessing) return;
        isProcessing = true;

        CompletableFuture.supplyAsync(() -> crate.getRandomItem())
            .thenAccept(itemOptional -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    this.currentItem = itemOptional;
                    displayRollResult();
                });
            })
            .exceptionally(throwable -> {
                plugin.getLogger().warning("Error during item roll: " + throwable.getMessage());
                isProcessing = false;
                return null;
            });
    }

    private void displayRollResult() {
        currentItem.ifPresentOrElse(
            this::handleSuccessfulRoll,
            this::handleEmptyCrate
        );

        isProcessing = false;
    }

    private void handleSuccessfulRoll(ItemStack item) {
        showRollAnimation(item);
        consumeKeyAsync();
        scheduleItemDelivery(item.clone());
    }

    private void handleEmptyCrate() {
        ChatHelper.showTitle(player, "<red>Błąd", "<gray>Skrzynka jest pusta!");
        inventory.setItem(ITEM_DISPLAY_SLOT, ItemBuilder.of(Material.BARRIER)
            .name("<red>Pusta Skrzynka")
            .lore("<gray>Ta skrzynka nie zawiera żadnych przedmiotów")
            .build());
    }

    private void showRollAnimation(ItemStack finalItem) {
        new BukkitRunnable() {
            private int ticksElapsed = 0;
            private final int animationDuration = ANIMATION_DELAY_TICKS;

            @Override
            public void run() {
                if (ticksElapsed >= animationDuration) {
                    inventory.setItem(ITEM_DISPLAY_SLOT, finalItem);
                    cancel();
                    return;
                }

                final ItemStack randomItem = getRandomDisplayItem();
                inventory.setItem(ITEM_DISPLAY_SLOT, randomItem);
                ticksElapsed++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private ItemStack getRandomDisplayItem() {
        return crate.getItems().isEmpty()
            ? new ItemStack(Material.STONE)
            : crate.getItems().get(ThreadLocalRandom.current().nextInt(crate.getItems().size()));
    }

    private void consumeKeyAsync() {
        CompletableFuture.runAsync(() -> {
            crate.getKeyItem().ifPresent(this::removeKeyFromInventory);
        });
    }

    private void removeKeyFromInventory(ItemStack requiredKey) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Arrays.stream(player.getInventory().getContents())
                .filter(item -> item != null && item.isSimilar(requiredKey))
                .findFirst()
                .ifPresent(matchingKey -> {
                    if (matchingKey.getAmount() > 1) {
                        matchingKey.setAmount(matchingKey.getAmount() - 1);
                    } else {
                        player.getInventory().remove(matchingKey);
                    }
                });
        });
    }

    private void scheduleItemDelivery(ItemStack item) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            giveItemToPlayer(item);
            updateInventoryButtons();
        }, ANIMATION_DELAY_TICKS + 5L);
    }

    private void giveItemToPlayer(ItemStack item) {
        final var leftover = player.getInventory().addItem(item);

        if (!leftover.isEmpty()) {
            leftover.values().forEach(leftoverItem ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftoverItem)
            );
            ChatHelper.showActionBar(player, "<yellow>Niektóre przedmioty zostały upuszczone na ziemię!");
        }
    }

    private void updateInventoryButtons() {
        setupInventory();
    }

    public void handleClick(int slot) {
        if (isProcessing) {
            ChatHelper.showActionBar(player, "<red>Poczekaj, trwa przetwarzanie...");
            return;
        }

        switch (slot) {
            case REOPEN_SLOT -> handleReopenClick();
            case CLOSE_SLOT -> player.closeInventory();
        }
    }

    private void handleReopenClick() {
        if (!hasValidKey()) {
            ChatHelper.showTitle(player, "<red>Brak Klucza",
                "<gray>Potrzebujesz klucza aby otworzyć ponownie");
            return;
        }

        if (crate.isEmpty()) {
            ChatHelper.showTitle(player, "<red>Pusta Skrzynka",
                "<gray>Ta skrzynka nie zawiera żadnych przedmiotów");
            return;
        }

        rollNewItemWithAnimation();
    }

    private boolean hasValidKey() {
        return crate.getKeyItem()
            .map(requiredKey ->
                Arrays.stream(player.getInventory().getContents())
                    .anyMatch(item -> item != null && item.isSimilar(requiredKey))
            )
            .orElse(true);
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        return inventory;
    }
}
