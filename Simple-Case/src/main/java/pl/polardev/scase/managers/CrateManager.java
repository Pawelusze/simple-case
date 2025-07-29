package pl.polardev.scase.managers;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;
import pl.polardev.scase.CasePlugin;
import pl.polardev.scase.models.Crate;
import net.kyori.adventure.text.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

public class CrateManager {
    private final CasePlugin plugin;
    private final Path dataFolder;
    private final Map<String, Crate> crates;
    private final Set<String> pendingSaves;
    private final BukkitTask autoSaveTask;
    private final Map<String, Long> fileModificationTimes;

    public CrateManager(CasePlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder().toPath().resolve("cases");
        this.crates = new ConcurrentHashMap<>();
        this.pendingSaves = ConcurrentHashMap.newKeySet();
        this.fileModificationTimes = new ConcurrentHashMap<>();

        try {
            Files.createDirectories(dataFolder);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create cases directory: " + e.getMessage());
        }

        loadCrates();

        this.autoSaveTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            saveAllPending();
            checkForDeletedFiles();
        }, 600L, 1200L);
    }

    public void createCrate(String name, Block block) {
        ItemStack crateItem = new ItemStack(block.getType());
        ItemMeta meta = crateItem.getItemMeta();
        meta.displayName(Component.text("§cCase " + name));
        crateItem.setItemMeta(meta);

        Crate crate = new Crate(name, crateItem);
        crates.put(name.toLowerCase(), crate);

        saveCrateSync(crate);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                if (block.getState() instanceof org.bukkit.block.TileState tileState) {
                    org.bukkit.NamespacedKey crateKey = new org.bukkit.NamespacedKey(plugin, "crate_name");
                    tileState.getPersistentDataContainer().set(crateKey, org.bukkit.persistence.PersistentDataType.STRING, name);
                    tileState.update();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to set persistent data for crate: " + e.getMessage());
            }
        });
    }

    public Crate getCrate(String name) {
        return crates.get(name.toLowerCase());
    }

    public void deleteCrate(String name) {
        String lowerName = name.toLowerCase();
        Crate crate = crates.get(lowerName);
        if (crate != null) {
            plugin.getServer().getScheduler().runTask(plugin, () -> removeAllCrateBlocks(name));
        }

        crates.remove(lowerName);
        pendingSaves.remove(lowerName);
        fileModificationTimes.remove(lowerName);

        CompletableFuture.runAsync(() -> {
            try {
                Files.deleteIfExists(dataFolder.resolve(lowerName + ".yml"));
                plugin.getLogger().info("Deleted crate file: " + lowerName + ".yml");
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to delete crate file: " + e.getMessage());
            }
        });
    }

    private void checkForDeletedFiles() {
        List<String> toRemove = new ArrayList<>();

        for (String crateName : crates.keySet()) {
            Path file = dataFolder.resolve(crateName + ".yml");
            if (!Files.exists(file)) {
                toRemove.add(crateName);
                plugin.getLogger().info("Detected deleted crate file: " + crateName + ".yml - removing from memory");
            }
        }

        for (String crateName : toRemove) {
            crates.remove(crateName);
            pendingSaves.remove(crateName);
            fileModificationTimes.remove(crateName);

            plugin.getServer().getScheduler().runTask(plugin, () -> removeAllCrateBlocks(crateName));
        }
    }

    public void markForSave(String crateName) {
        pendingSaves.add(crateName.toLowerCase());
    }

    private void saveAllPending() {
        if (pendingSaves.isEmpty()) return;

        Set<String> toSave = new HashSet<>(pendingSaves);
        pendingSaves.clear();

        CompletableFuture.runAsync(() -> {
            for (String crateName : toSave) {
                Crate crate = crates.get(crateName);
                if (crate != null) {
                    saveCrateSync(crate);
                }
            }
        });
    }

    private void removeAllCrateBlocks(String crateName) {
        org.bukkit.NamespacedKey crateKey = new org.bukkit.NamespacedKey(plugin, "crate_name");

        try {
            for (org.bukkit.World world : plugin.getServer().getWorlds()) {
                for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                    try {
                        for (org.bukkit.block.BlockState state : chunk.getTileEntities()) {
                            try {
                                if (state instanceof org.bukkit.block.TileState tileState) {
                                    String blockCrateName = tileState.getPersistentDataContainer()
                                        .get(crateKey, org.bukkit.persistence.PersistentDataType.STRING);

                                    if (crateName.equals(blockCrateName)) {
                                        tileState.getPersistentDataContainer().remove(crateKey);
                                        tileState.update();
                                    }
                                }
                            } catch (Exception e) {
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error while removing crate blocks: " + e.getMessage());
        }
    }

    public Set<String> getCrateNames() {
        return new HashSet<>(crates.keySet());
    }

    public void saveCrate(Crate crate) {
        markForSave(crate.getName());
    }

    public void saveCrateImmediately(Crate crate) {
        saveCrateSync(crate);
    }

    private void saveCrateSync(Crate crate) {
        try {
            Path file = dataFolder.resolve(crate.getName().toLowerCase() + ".yml");
            YamlConfiguration config = new YamlConfiguration();

            config.set("name", crate.getName());
            config.set("displayItem", optimizeItemStack(crate.getDisplayItem()));

            if (crate.getKeyItem() != null) {
                config.set("keyItem", optimizeItemStack(crate.getKeyItem()));
            }

            List<ItemStack> items = crate.getItems();
            if (!items.isEmpty()) {
                List<ItemStack> optimizedItems = new ArrayList<>();
                for (ItemStack item : items) {
                    optimizedItems.add(optimizeItemStack(item));
                }
                config.set("items", optimizedItems);
            }

            config.save(file.toFile());

            fileModificationTimes.put(crate.getName().toLowerCase(), System.currentTimeMillis());

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save crate " + crate.getName() + ": " + e.getMessage());
        }
    }

    private ItemStack optimizeItemStack(ItemStack original) {
        if (original == null) return null;

        ItemStack optimized = original.clone();
        ItemMeta meta = optimized.getItemMeta();

        if (meta != null) {
            if (meta.lore() != null && meta.lore().isEmpty()) {
                meta.lore(null);
            }

            if (meta.getPersistentDataContainer().isEmpty()) {
            }

            optimized.setItemMeta(meta);
        }

        return optimized;
    }

    public void saveAll() {
        saveAllPending();

        for (Crate crate : crates.values()) {
            saveCrateSync(crate);
        }
    }

    public void shutdown() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        saveAll();
    }

    private void loadCrates() {
        try {
            try (var stream = Files.list(dataFolder)) {
                stream.filter(path -> path.toString().endsWith(".yml"))
                      .forEach(this::loadCrate);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load crates: " + e.getMessage());
        }
    }

    private void loadCrate(Path file) {
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file.toFile());

            String name = config.getString("name");
            ItemStack displayItem = config.getItemStack("displayItem");
            ItemStack keyItem = config.getItemStack("keyItem");

            if (name != null && displayItem != null) {
                Crate crate = new Crate(name, displayItem);
                if (keyItem != null) {
                    crate.setKeyItem(keyItem);
                }

                @SuppressWarnings("unchecked")
                List<ItemStack> items = (List<ItemStack>) config.getList("items", new ArrayList<>());
                crate.setItems(items);

                crates.put(crate.getName().toLowerCase(), crate);
                fileModificationTimes.put(crate.getName().toLowerCase(), System.currentTimeMillis());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load crate from " + file.getFileName() + ": " + e.getMessage());
        }
    }
}
