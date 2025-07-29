package pl.polardev.scase.manager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import pl.polardev.scase.CasePlugin;
import pl.polardev.scase.helper.ItemBuilder;
import pl.polardev.scase.model.Crate;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class CrateManager {

    public static class CrateValidationException extends Exception {
        public CrateValidationException(String message) {
            super(message);
        }
    }

    private final CasePlugin plugin;
    private final File dataFolder;
    private final NamespacedKey crateKey;
    private final Map<String, Crate> crates;
    private final Map<String, NamespacedKey> keyCache;
    private final Map<UUID, Map<String, Integer>> playerKeyCache;
    private static final int CACHE_CLEANUP_INTERVAL = 6000;
    private int cacheCleanupCounter = 0;

    public CrateManager(CasePlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "cases");
        this.crateKey = new NamespacedKey(plugin, "crate_name");
        this.crates = new ConcurrentHashMap<>();
        this.keyCache = new ConcurrentHashMap<>();
        this.playerKeyCache = new ConcurrentHashMap<>();

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        loadAllCrates();
        startCacheCleanupTask();
    }

    private void startCacheCleanupTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (++cacheCleanupCounter >= CACHE_CLEANUP_INTERVAL) {
                cleanupCaches();
                cacheCleanupCounter = 0;
            }
        }, 1200L, 1200L);
    }

    private void cleanupCaches() {
        playerKeyCache.entrySet().removeIf(entry ->
            plugin.getServer().getPlayer(entry.getKey()) == null);
    }

    public void createCrate(String name, Block block) throws CrateValidationException {
        validateCrateName(name);

        if (crates.containsKey(name.toLowerCase())) {
            throw new CrateValidationException("Crate '" + name + "' already exists");
        }

        ItemStack crateItem = ItemBuilder.of(block.getType())
            .name("<red>Case " + name)
            .build();

        Crate crate = new Crate(name, crateItem);
        crates.put(name.toLowerCase(), crate);

        saveCrate(crate);
        setCrateBlock(block, name);
    }

    public void setCrateBlock(Block block, String crateName) {
        if (block.getState() instanceof org.bukkit.block.TileState tileState) {
            tileState.getPersistentDataContainer().set(crateKey, PersistentDataType.STRING, crateName);
            tileState.update();
        } else {
            plugin.getLogger().warning("Cannot set crate data on block type: " + block.getType() + " - not a tile entity");
        }
    }

    public boolean deleteCrate(String name) {
        String lowerName = name.toLowerCase();
        if (!crates.containsKey(lowerName)) {
            return false;
        }

        crates.remove(lowerName);

        File crateFile = new File(dataFolder, name + ".yml");
        if (crateFile.exists()) {
            crateFile.delete();
        }

        return true;
    }

    public Crate getCrate(String name) {
        return crates.get(name.toLowerCase());
    }

    public Set<String> getCrateNames() {
        return new HashSet<>(crates.keySet());
    }

    public boolean crateExists(String name) {
        return crates.containsKey(name.toLowerCase());
    }

    public String getCrateNameFromBlock(Block block) {
        if (block.getState() instanceof org.bukkit.block.TileState tileState) {
            return tileState.getPersistentDataContainer().get(crateKey, PersistentDataType.STRING);
        }
        return null;
    }

    public boolean hasKey(Player player, String crateName) {
        return getKeyAmount(player, crateName) > 0;
    }

    public int getKeyAmount(Player player, String crateName) {
        UUID playerId = player.getUniqueId();
        String lowerCrateName = crateName.toLowerCase();

        Map<String, Integer> playerKeys = playerKeyCache.get(playerId);
        if (playerKeys != null && playerKeys.containsKey(lowerCrateName)) {
            return playerKeys.get(lowerCrateName);
        }

        NamespacedKey keyKey = getOrCreateKeyNamespace(lowerCrateName);
        int amount = player.getPersistentDataContainer().getOrDefault(keyKey, PersistentDataType.INTEGER, 0);

        playerKeys = playerKeyCache.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        playerKeys.put(lowerCrateName, amount);

        return amount;
    }

    public void giveKey(Player player, String crateName, int amount) {
        if (amount <= 0) return;

        UUID playerId = player.getUniqueId();
        String lowerCrateName = crateName.toLowerCase();

        int currentAmount = getKeyAmount(player, crateName);
        int newAmount = currentAmount + amount;

        NamespacedKey keyKey = getOrCreateKeyNamespace(lowerCrateName);
        player.getPersistentDataContainer().set(keyKey, PersistentDataType.INTEGER, newAmount);

        Map<String, Integer> playerKeys = playerKeyCache.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        playerKeys.put(lowerCrateName, newAmount);
    }

    public void removeKey(Player player, String crateName, int amount) {
        if (amount <= 0) return;

        UUID playerId = player.getUniqueId();
        String lowerCrateName = crateName.toLowerCase();

        int currentAmount = getKeyAmount(player, crateName);
        int newAmount = Math.max(0, currentAmount - amount);

        NamespacedKey keyKey = getOrCreateKeyNamespace(lowerCrateName);
        player.getPersistentDataContainer().set(keyKey, PersistentDataType.INTEGER, newAmount);

        Map<String, Integer> playerKeys = playerKeyCache.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        playerKeys.put(lowerCrateName, newAmount);
    }

    private NamespacedKey getOrCreateKeyNamespace(String crateName) {
        return keyCache.computeIfAbsent(crateName,
            name -> new NamespacedKey(plugin, "crate_key_" + name));
    }

    public void invalidatePlayerCache(Player player) {
        playerKeyCache.remove(player.getUniqueId());
    }

    private void validateCrateName(String name) throws CrateValidationException {
        if (name == null || name.trim().isEmpty()) {
            throw new CrateValidationException("Crate name cannot be empty");
        }

        if (name.length() > 32) {
            throw new CrateValidationException("Crate name cannot be longer than 32 characters");
        }

        if (!name.matches("[a-zA-Z0-9_-]+")) {
            throw new CrateValidationException("Crate name can only contain letters, numbers, underscores and hyphens");
        }
    }

    public void loadAllCrates() {
        crates.clear();

        if (!dataFolder.exists()) {
            return;
        }

        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String name = file.getName().replace(".yml", "");

                Crate crate = loadCrateFromConfig(name, config);
                if (crate != null) {
                    crates.put(name.toLowerCase(), crate);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load crate from file: " + file.getName(), e);
            }
        }

        plugin.getLogger().info("Loaded " + crates.size() + " crates");
    }

    private Crate loadCrateFromConfig(String name, YamlConfiguration config) {
        try {
            ItemStack displayItem = config.getItemStack("display-item");
            if (displayItem == null) {
                plugin.getLogger().warning("No display item found for crate: " + name);
                return null;
            }

            Crate crate = new Crate(name, displayItem);

            ItemStack keyItem = config.getItemStack("key-item");
            if (keyItem != null) {
                crate.setKeyItem(keyItem);
            }

            ConfigurationSection itemsSection = config.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String key : itemsSection.getKeys(false)) {
                    ItemStack item = itemsSection.getItemStack(key);
                    if (item != null) {
                        crate.addItem(item);
                    }
                }
            }

            return crate;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error loading crate: " + name, e);
            return null;
        }
    }

    public void saveCrate(Crate crate) {
        try {
            File crateFile = new File(dataFolder, crate.getName() + ".yml");
            YamlConfiguration config = new YamlConfiguration();

            config.set("name", crate.getName());
            config.set("display-item", crate.getDisplayItem());

            // Save key item if exists
            if (crate.getKeyItem() != null) {
                config.set("key-item", crate.getKeyItem());
            }

            List<ItemStack> items = crate.getItems();
            for (int i = 0; i < items.size(); i++) {
                config.set("items." + i, items.get(i));
            }

            config.save(crateFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save crate: " + crate.getName(), e);
        }
    }

    public void saveAllCrates() {
        for (Crate crate : crates.values()) {
            saveCrate(crate);
        }
    }

    public void reload() {
        loadAllCrates();
    }
}
