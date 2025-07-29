package pl.polardev.scase.manager;

import org.bukkit.entity.Player;
import pl.polardev.scase.CasePlugin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitManager {
    private final CasePlugin plugin;
    private final ConcurrentHashMap<UUID, Long> playerInteractionCooldowns;
    private final ConcurrentHashMap<UUID, Long> playerGUIOpenCooldowns;
    private final ConcurrentHashMap<UUID, Integer> playerClickCounts;

    private static final long INTERACTION_COOLDOWN_MS = 100;
    private static final long GUI_OPEN_COOLDOWN_MS = 500;
    private static final int MAX_CLICKS_PER_SECOND = 10;
    private static final long CLICK_WINDOW_MS = 1000;

    public RateLimitManager(CasePlugin plugin) {
        this.plugin = plugin;
        this.playerInteractionCooldowns = new ConcurrentHashMap<>();
        this.playerGUIOpenCooldowns = new ConcurrentHashMap<>();
        this.playerClickCounts = new ConcurrentHashMap<>();

        startCleanupTask();
    }

    private void startCleanupTask() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long currentTime = System.currentTimeMillis();

            playerInteractionCooldowns.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > INTERACTION_COOLDOWN_MS * 10);
            playerGUIOpenCooldowns.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > GUI_OPEN_COOLDOWN_MS * 10);
            playerClickCounts.clear();

        }, 600L, 600L);
    }

    public boolean canInteract(Player player) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastInteraction = playerInteractionCooldowns.get(playerId);

        if (lastInteraction != null && currentTime - lastInteraction < INTERACTION_COOLDOWN_MS) {
            return false;
        }

        playerInteractionCooldowns.put(playerId, currentTime);
        return true;
    }

    public boolean canOpenGUI(Player player) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastGUIOpen = playerGUIOpenCooldowns.get(playerId);

        if (lastGUIOpen != null && currentTime - lastGUIOpen < GUI_OPEN_COOLDOWN_MS) {
            return false;
        }

        playerGUIOpenCooldowns.put(playerId, currentTime);
        return true;
    }

    public boolean isClickSpamming(Player player) {
        UUID playerId = player.getUniqueId();
        int currentClicks = playerClickCounts.getOrDefault(playerId, 0);

        if (currentClicks >= MAX_CLICKS_PER_SECOND) {
            return true;
        }

        playerClickCounts.put(playerId, currentClicks + 1);
        return false;
    }

    public void shutdown() {
        playerInteractionCooldowns.clear();
        playerGUIOpenCooldowns.clear();
        playerClickCounts.clear();
    }
}
