package pl.polardev.scase.managers;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import pl.polardev.scase.CasePlugin;
import pl.polardev.scase.helpers.ItemBuilder;
import pl.polardev.scase.models.Crate;
import net.kyori.adventure.text.minimessage.MiniMessage;

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
    private final NamespacedKey crateKey;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public CrateManager(CasePlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder().toPath().resolve("cases");
        this.crates = new ConcurrentHashMap<>();
        this.pendingSaves = ConcurrentHashMap.newKeySet();
        this.crateKey = new NamespacedKey(plugin, "crate_name");

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
        ItemStack crateItem = ItemBuilder.of(block.getType())
            .name("<red>Case " + name)
            .build();

        Crate crate = new Crate(name, crateItem);
        crates.put(name.toLowerCase(), crate);

        saveCrateSync(crate);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                if (block.getState() instanceof org.bukkit.block.TileState tileState) {
                    tileState.getPersistentDataContainer().set(crateKey, PersistentDataType.STRING, name);
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
            plugin.getServer().getScheduler().runTask(plugin, () -> removeCrateBlocks(name));
        }

        crates.remove(lowerName);
        pendingSaves.remove(lowerName);

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
            }
        }

        for (String crateName : toRemove) {
            crates.remove(crateName);
            pendingSaves.remove(crateName);

            plugin.getServer().getScheduler().runTask(plugin, () -> removeCrateBlocks(crateName));
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

    private void removeCrateBlocks(String crateName) {
        Map<String, Set<org.bukkit.Location>> crateLocations = new HashMap<>();

        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            crateLocations.put(world.getName(), new HashSet<>());
        }

        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                for (org.bukkit.block.BlockState state : chunk.getTileEntities()) {
                    if (state instanceof org.bukkit.block.TileState tileState) {
                        String blockCrateName = tileState.getPersistentDataContainer()
                            .get(crateKey, PersistentDataType.STRING);

                        if (crateName.equals(blockCrateName)) {
                            crateLocations.get(world.getName()).add(state.getLocation());
                        }
                    }
                }
            }
        }

        for (Map.Entry<String, Set<org.bukkit.Location>> entry : crateLocations.entrySet()) {
            org.bukkit.World world = plugin.getServer().getWorld(entry.getKey());
            if (world != null) {
                for (org.bukkit.Location loc : entry.getValue()) {
                    org.bukkit.block.Block block = world.getBlockAt(loc);
                    if (block.getState() instanceof org.bukkit.block.TileState tileState) {
                        tileState.getPersistentDataContainer().remove(crateKey);
                        tileState.update();
                    }
                }
            }
        }
    }

    public Set<String> getCrateNames() {
        return Set.copyOf(crates.keySet());
    }

    public void saveCrate(Crate crate) {
        markForSave(crate.getName());
    }

    public void saveCrateImmediately(Crate crate) {
        saveCrateSync(crate);
    }

    private void saveCrateSync(Crate crate) {
        final Path file = dataFolder.resolve(crate.getName().toLowerCase() + ".yml");
        final YamlConfiguration config = new YamlConfiguration();

        populateConfig(config, crate);

        try {
            config.save(file.toFile());
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save crate %s: %s".formatted(crate.getName(), e.getMessage()));
        }
    }

    private void populateConfig(YamlConfiguration config, Crate crate) {
        config.set("name", crate.getName());
        config.set("displayItem", crate.getDisplayItem());

        crate.getKeyItem().ifPresent(keyItem -> config.set("keyItem", keyItem));

        final List<ItemStack> items = crate.getItems();
        if (!items.isEmpty()) {
            config.set("items", items);
        }
    }

    public void saveAll() {
        saveAllPending();

        final List<Crate> cratesToSave = List.copyOf(crates.values());
        CompletableFuture.runAsync(() ->
            cratesToSave.parallelStream()
                .forEach(this::saveCrateSync)
        ).exceptionally(throwable -> {
            plugin.getLogger().severe("Failed to save crates in batch: " + throwable.getMessage());
            return null;
        });
    }

    public void shutdown() {
        plugin.getLogger().info("Shutting down CrateManager...");

        final long startTime = System.currentTimeMillis();

        try {
            Optional.ofNullable(autoSaveTask)
                    .filter(task -> !task.isCancelled())
                    .ifPresent(BukkitTask::cancel);

            saveAll();

            final long shutdownTime = System.currentTimeMillis() - startTime;
            plugin.getLogger().info("CrateManager shutdown completed in {}ms. Saved {} crates."
                    .formatted(shutdownTime, crates.size()));

        } catch (Exception e) {
            plugin.getLogger().severe("Critical error during CrateManager shutdown: " + e.getMessage());
            e.printStackTrace();
        }
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
                if (!items.isEmpty()) {
                    crate.setItems(items);
                }

                crates.put(crate.getName().toLowerCase(), crate);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load crate from " + file.getFileName() + ": " + e.getMessage());
        }
    }
}
