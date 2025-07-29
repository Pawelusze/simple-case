package pl.polardev.scase.guis;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import pl.polardev.scase.CasePlugin;
import pl.polardev.scase.helpers.ChatHelper;
import pl.polardev.scase.models.Crate;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class CrateAnimationGUI implements InventoryHolder {
    private final CasePlugin plugin;
    private final Player player;
    private final Crate crate;
    private final Inventory inventory;

    private final List<ItemStack> animationItems;
    private final ItemStack finalItem;
    private BukkitTask animationTask;
    private int currentPosition = 0;
    private long animationSpeed = 5L;
    private int ticksElapsed = 0;
    private boolean animationRunning = false;

    private static final int ANIMATION_ITEMS_COUNT = 200;
    private static final int ANIMATION_DURATION_TICKS = 140;
    private static final int ANIMATION_ROW_START = 9;
    private static final int ANIMATION_ROW_SIZE = 9;
    private static final int FINAL_ITEM_SLOT = 13;
    private static final int AUTO_CLOSE_DELAY_TICKS = 60;

    public CrateAnimationGUI(CasePlugin plugin, Player player, Crate crate) {
        this.plugin = plugin;
        this.player = player;
        this.crate = crate;
        this.inventory = Bukkit.createInventory(this, 27, ChatHelper.deserialize("<gold>Otwieranie z animacją: " + crate.getName()));
        this.animationItems = generateAnimationItems();
        this.finalItem = crate.getRandomItem().orElse(new ItemStack(Material.STONE));
    }

    private List<ItemStack> generateAnimationItems() {
        final List<ItemStack> crateItems = crate.getItems();

        if (crateItems.isEmpty()) {
            return List.of(new ItemStack(Material.STONE));
        }

        return IntStream.range(0, ANIMATION_ITEMS_COUNT)
                .mapToObj(i -> crateItems.get(ThreadLocalRandom.current().nextInt(crateItems.size())))
                .toList();
    }

    public void open() {
        player.openInventory(inventory);
        consumeKey();
        startAnimation();
    }

    private void consumeKey() {
        crate.getKeyItem().ifPresent(requiredKey -> {
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

    private void startAnimation() {
        if (animationRunning) return;
        animationRunning = true;

        animationTask = new BukkitRunnable() {
            @Override
            public void run() {
                ticksElapsed++;

                if (ticksElapsed >= ANIMATION_DURATION_TICKS) {
                    finishAnimation();
                    return;
                }

                updateAnimationSpeed();
                updateInventoryDisplay();
                currentPosition = (currentPosition + 1) % animationItems.size();
            }
        }.runTaskTimer(plugin, 0L, getInitialAnimationSpeed());
    }

    private void updateAnimationSpeed() {
        final long newSpeed = switch (ticksElapsed / 20) {
            case 0, 1 -> 3L;        // 0-39 ticks
            case 2, 3 -> 5L;        // 40-79 ticks
            case 4 -> 8L;           // 80-99 ticks
            case 5 -> 12L;          // 100-119 ticks
            default -> 20L;         // 120+ ticks
        };

        if (animationSpeed != newSpeed) {
            animationSpeed = newSpeed;
            restartAnimationWithNewSpeed();
        }
    }

    private long getInitialAnimationSpeed() {
        return 3L;
    }

    private void restartAnimationWithNewSpeed() {
        if (animationTask != null) {
            animationTask.cancel();
        }

        animationTask = new BukkitRunnable() {
            @Override
            public void run() {
                ticksElapsed++;

                if (ticksElapsed >= ANIMATION_DURATION_TICKS) {
                    finishAnimation();
                    return;
                }

                updateAnimationSpeed();
                updateInventoryDisplay();
                currentPosition = (currentPosition + 1) % animationItems.size();
            }
        }.runTaskTimer(plugin, 0L, animationSpeed);
    }

    private void updateInventoryDisplay() {
        inventory.clear();

        IntStream.range(0, ANIMATION_ROW_SIZE)
                .forEach(slot -> {
                    final int itemIndex = (currentPosition + slot) % animationItems.size();
                    inventory.setItem(ANIMATION_ROW_START + slot, animationItems.get(itemIndex));
                });
    }

    private void finishAnimation() {
        Optional.ofNullable(animationTask).ifPresent(BukkitTask::cancel);

        inventory.clear();
        inventory.setItem(FINAL_ITEM_SLOT, finalItem);

        giveItemToPlayer(finalItem.clone());

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.getOpenInventory().getTopInventory().equals(inventory)) {
                player.closeInventory();
            }
        }, AUTO_CLOSE_DELAY_TICKS);

        animationRunning = false;
    }

    private void giveItemToPlayer(ItemStack item) {
        final var leftover = player.getInventory().addItem(item);

        leftover.values()
                .forEach(leftoverItem ->
                    player.getWorld().dropItemNaturally(player.getLocation(), leftoverItem)
                );
    }

    public void onClose() {
        Optional.ofNullable(animationTask).ifPresent(BukkitTask::cancel);
        animationRunning = false;
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        return inventory;
    }
}
