package pl.polardev.scase.manager;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import pl.polardev.scase.CasePlugin;
import pl.polardev.scase.inventory.CrateAnimationInventory;
import pl.polardev.scase.model.Crate;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

public class AnimationManager {
    private final CasePlugin plugin;
    private final Map<UUID, AnimationInstance> activeAnimations;
    private BukkitTask globalAnimationTask;

    private static final int[] ANIMATION_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final int ANIMATION_DURATION = 140;

    public AnimationManager(CasePlugin plugin) {
        this.plugin = plugin;
        this.activeAnimations = new ConcurrentHashMap<>();
        startGlobalAnimationTask();
    }

    private void startGlobalAnimationTask() {
        if (globalAnimationTask != null) return;

        globalAnimationTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (activeAnimations.isEmpty()) return;

            activeAnimations.values().parallelStream().forEach(this::processAnimation);
            activeAnimations.entrySet().removeIf(entry -> entry.getValue().isCompleted());
        }, 1L, 1L);
    }

    public void startAnimation(Player player, CrateAnimationInventory gui, Crate crate) {
        UUID playerId = player.getUniqueId();

        ItemStack[] preGeneratedItems = new ItemStack[ANIMATION_DURATION + 10];
        ItemStack winningItem = crate.getRandomItem();

        for (int i = 0; i < preGeneratedItems.length - 10; i++) {
            preGeneratedItems[i] = crate.getRandomItem();
        }

        for (int i = preGeneratedItems.length - 10; i < preGeneratedItems.length; i++) {
            preGeneratedItems[i] = winningItem;
        }

        AnimationInstance instance = new AnimationInstance(gui, preGeneratedItems, winningItem);
        activeAnimations.put(playerId, instance);
    }

    private void processAnimation(AnimationInstance instance) {
        if (instance.isCompleted()) return;

        int tick = instance.getCurrentTick();
        long speed = getAnimationSpeed(tick);

        if (tick % speed == 0) {
            instance.updateAnimation();
        }

        instance.incrementTick();

        if (tick >= ANIMATION_DURATION) {
            instance.finishAnimation();
        }
    }

    private long getAnimationSpeed(int tick) {
        if (tick < 40) return 3L;
        if (tick < 80) return 5L;
        if (tick < 100) return 8L;
        if (tick < 120) return 12L;
        return 20L;
    }

    public void stopAnimation(Player player) {
        activeAnimations.remove(player.getUniqueId());
    }

    public void shutdown() {
        if (globalAnimationTask != null) {
            globalAnimationTask.cancel();
        }
        activeAnimations.clear();
    }

    private static class AnimationInstance {
        private final CrateAnimationInventory gui;
        private final ItemStack[] preGeneratedItems;
        private final ItemStack winningItem;
        private int currentTick = 0;
        private int itemIndex = 0;
        private boolean completed = false;

        public AnimationInstance(CrateAnimationInventory gui, ItemStack[] preGeneratedItems, ItemStack winningItem) {
            this.gui = gui;
            this.preGeneratedItems = preGeneratedItems;
            this.winningItem = winningItem;
        }

        public void updateAnimation() {
            for (int i = 0; i < ANIMATION_SLOTS.length; i++) {
                int itemIdx = (itemIndex + i) % preGeneratedItems.length;
                ItemStack item = preGeneratedItems[itemIdx];
                if (item != null) {
                    gui.getInventory().setItem(ANIMATION_SLOTS[i], item);
                }
            }
            itemIndex = (itemIndex + 1) % preGeneratedItems.length;
        }

        public void finishAnimation() {
            if (completed) return;
            completed = true;
            gui.finishAnimation(winningItem);
        }

        public int getCurrentTick() {
            return currentTick;
        }

        public void incrementTick() {
            currentTick++;
        }

        public boolean isCompleted() {
            return completed;
        }
    }
}
