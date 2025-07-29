package pl.polardev.scase.guis;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import pl.polardev.scase.CasePlugin;
import pl.polardev.scase.models.Crate;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class CrateAnimationGUI implements InventoryHolder {
    private final CasePlugin plugin;
    private final Player player;
    private final Crate crate;
    private final Inventory inventory;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private final List<ItemStack> rouletteItems;
    private final ItemStack winningItem;
    private BukkitTask animationTask;
    private int currentPosition = 0;
    private int animationTicks = 0;
    private boolean animationRunning = false;

    private static final int TOTAL_ANIMATION_TICKS = 140;
    private static final int[] DISPLAY_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final int ROULETTE_SIZE = 50;

    public CrateAnimationGUI(CasePlugin plugin, Player player, Crate crate) {
        this.plugin = plugin;
        this.player = player;
        this.crate = crate;
        this.inventory = Bukkit.createInventory(this, 27, mm.deserialize("<gold>✨ " + crate.getName() + " - Roulette"));

        this.winningItem = crate.getRandomItem();
        this.rouletteItems = prepareRouletteItems();

        setupInventory();
    }

    private List<ItemStack> prepareRouletteItems() {
        List<ItemStack> items = new ArrayList<>(ROULETTE_SIZE);
        List<ItemStack> crateItems = crate.getItems();

        if (crateItems.isEmpty()) {
            ItemStack placeholder = new ItemStack(Material.BARRIER);
            ItemMeta meta = placeholder.getItemMeta();
            meta.displayName(mm.deserialize("<red>Pusta skrzynka"));
            placeholder.setItemMeta(meta);
            for (int i = 0; i < ROULETTE_SIZE; i++) {
                items.add(placeholder);
            }
            return items;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < ROULETTE_SIZE; i++) {
            items.add(crateItems.get(random.nextInt(crateItems.size())).clone());
        }

        int winningPosition = 38;
        items.set(winningPosition, winningItem.clone());

        return items;
    }

    private void setupInventory() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(mm.deserialize(" "));
        glass.setItemMeta(glassMeta);

        for (int i = 0; i < 27; i++) {
            if (!isDisplaySlot(i)) {
                inventory.setItem(i, glass);
            }
        }

        ItemStack arrow = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta arrowMeta = arrow.getItemMeta();
        arrowMeta.displayName(mm.deserialize("<yellow>⬇ WINNER ⬇"));
        arrowMeta.lore(List.of(mm.deserialize("<gray>Przedmiot zatrzyma się tutaj")));
        arrow.setItemMeta(arrowMeta);
        inventory.setItem(4, arrow);

        updateDisplaySlots();
    }

    private boolean isDisplaySlot(int slot) {
        for (int displaySlot : DISPLAY_SLOTS) {
            if (slot == displaySlot) return true;
        }
        return false;
    }

    private void updateDisplaySlots() {
        for (int i = 0; i < DISPLAY_SLOTS.length; i++) {
            int itemIndex = (currentPosition + i) % ROULETTE_SIZE;
            inventory.setItem(DISPLAY_SLOTS[i], rouletteItems.get(itemIndex));
        }
    }

    public void open() {
        if (!consumeKey()) {
            player.sendMessage(mm.deserialize("<red>Brak klucza! Potrzebujesz odpowiedniego klucza aby otworzyć tę skrzynkę."));
            return;
        }

        player.openInventory(inventory);
        startAnimation();
    }

    private boolean consumeKey() {
        ItemStack requiredKey = crate.getKeyItem();
        if (requiredKey == null) return true;

        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
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

    private void startAnimation() {
        if (animationRunning) return;

        animationRunning = true;
        animationTicks = 0;
        currentPosition = 0;

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 0.8f);

        animationTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (animationTicks >= TOTAL_ANIMATION_TICKS) {
                    finishAnimation();
                    return;
                }

                int speed = calculateAnimationSpeed();

                if (animationTicks % speed == 0) {
                    currentPosition++;
                    updateDisplaySlots();

                    if (animationTicks < TOTAL_ANIMATION_TICKS - 28) {
                        float pitch = 0.5f + (animationTicks / (float)TOTAL_ANIMATION_TICKS);
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, pitch);
                    }
                }

                animationTicks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private int calculateAnimationSpeed() {
        float progress = animationTicks / (float)TOTAL_ANIMATION_TICKS;

        if (progress < 0.3f) return 2;
        if (progress < 0.6f) return 4;
        if (progress < 0.85f) return 8;
        return 15;
    }

    private void finishAnimation() {
        if (animationTask != null) {
            animationTask.cancel();
            animationTask = null;
        }

        animationRunning = false;

        currentPosition = 35;
        updateDisplaySlots();

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        addItemToInventory(winningItem);
        showWinTitle();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.getOpenInventory().getTopInventory().equals(inventory)) {
                player.closeInventory();
            }
        }, 60L);
    }

    private void addItemToInventory(ItemStack item) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
        if (!leftover.isEmpty()) {
            for (ItemStack leftoverItem : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftoverItem);
            }
        }
    }

    private void showWinTitle() {
        String itemName = winningItem.hasItemMeta() && winningItem.getItemMeta().hasDisplayName()
            ? winningItem.getItemMeta().getDisplayName()
            : winningItem.getType().name();

        player.showTitle(Title.title(
            mm.deserialize("<gold>✨ WYGRAŁEŚ! ✨"),
            mm.deserialize("<green>" + itemName),
            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
        ));
    }

    public void handleClick(int slot) {
    }

    public void onClose() {
        if (animationTask != null && !animationTask.isCancelled()) {
            animationTask.cancel();
            animationTask = null;
        }
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        return inventory;
    }
}
