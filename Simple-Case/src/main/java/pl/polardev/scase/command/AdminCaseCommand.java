package pl.polardev.scase.command;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.polardev.scase.CasePlugin;
import pl.polardev.scase.inventory.CrateEditInventory;
import pl.polardev.scase.helper.ChatHelper;
import pl.polardev.scase.manager.CrateManager;
import pl.polardev.scase.model.Crate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AdminCaseCommand implements TabExecutor {
    private final CasePlugin plugin;
    private static final Set<String> SUBCOMMANDS = Set.of("create", "edit", "delete", "setkey", "givekey", "setcase", "reload");
    private static final Set<String> CRATE_REQUIRING_COMMANDS = Set.of("edit", "delete", "setkey", "givekey", "setcase");
    private static final int MAX_KEYS_PER_COMMAND = 10000;

    public AdminCaseCommand(CasePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("simplecase.admin")) {
            ChatHelper.showTitle(player, "<red>No Permission", "<gray>You don't have permission to use this command");
            return true;
        }

        if (args.length == 0) {
            ChatHelper.showTitle(player, "<gold>Usage", "<gray>/admincase {create|edit|delete|setkey|givekey|setcase|reload}");
            return true;
        }

        String subcommand = args[0].toLowerCase();

        return switch (subcommand) {
            case "create" -> handleCreate(player, args);
            case "edit" -> handleEdit(player, args);
            case "delete" -> handleDelete(player, args);
            case "setkey" -> handleSetKey(player, args);
            case "givekey" -> handleGiveKey(player, args);
            case "setcase" -> handleSetCase(player, args);
            case "reload" -> handleReload(player);
            default -> {
                ChatHelper.showTitle(player, "<red>Invalid Command", "<gray>Use /admincase {create|edit|delete|setkey|givekey|setcase|reload}");
                yield true;
            }
        };
    }

    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            ChatHelper.showTitle(player, "<red>Usage", "<gray>/admincase create <name>");
            return true;
        }

        String name = args[1];
        Block targetBlock = player.getTargetBlockExact(5);

        if (targetBlock == null || targetBlock.getType() == Material.AIR) {
            ChatHelper.showTitle(player, "<red>Error", "<gray>Look at a block to create a crate");
            return true;
        }

        try {
            plugin.getCrateManager().createCrate(name, targetBlock);
            ChatHelper.showTitle(player, "<green>Success", "<gray>Crate <gold>" + name + "<gray> created successfully");
        } catch (CrateManager.CrateValidationException e) {
            ChatHelper.showTitle(player, "<red>Error", "<gray>" + e.getMessage());
        }

        return true;
    }

    private boolean handleEdit(Player player, String[] args) {
        if (args.length < 2) {
            ChatHelper.showTitle(player, "<red>Usage", "<gray>/admincase edit <name>");
            return true;
        }

        String crateName = args[1];
        Crate crate = plugin.getCrateManager().getCrate(crateName);

        if (crate == null) {
            ChatHelper.showTitle(player, "<red>Error", "<gray>Crate <gold>" + crateName + "<gray> not found");
            return true;
        }

        new CrateEditInventory(plugin, player, crate).open();
        return true;
    }

    private boolean handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            ChatHelper.showTitle(player, "<red>Usage", "<gray>/admincase delete <name>");
            return true;
        }

        String crateName = args[1];

        if (plugin.getCrateManager().deleteCrate(crateName)) {
            ChatHelper.showTitle(player, "<green>Success", "<gray>Crate <gold>" + crateName + "<gray> deleted successfully");
        } else {
            ChatHelper.showTitle(player, "<red>Error", "<gray>Crate <gold>" + crateName + "<gray> not found");
        }

        return true;
    }

    private boolean handleSetKey(Player player, String[] args) {
        if (args.length < 2) {
            ChatHelper.showTitle(player, "<red>Usage", "<gray>/admincase setkey <crate>");
            return true;
        }

        String crateName = args[1];

        if (!plugin.getCrateManager().crateExists(crateName)) {
            ChatHelper.showTitle(player, "<red>Error", "<gray>Crate <gold>" + crateName + "<gray> not found");
            return true;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() == Material.AIR) {
            ChatHelper.showTitle(player, "<red>Error", "<gray>Musisz trzymać przedmiot w ręce");
            return true;
        }

        Crate crate = plugin.getCrateManager().getCrate(crateName);
        if (crate != null) {
            crate.setKeyItem(itemInHand);
            plugin.getCrateManager().saveCrate(crate);
            ChatHelper.showTitle(player, "<green>Success", "<gray>Klucz dla skrzynki <gold>" + crateName + "<gray> został ustawiony");
        }

        return true;
    }

    private boolean handleGiveKey(Player player, String[] args) {
        if (args.length < 4) {
            ChatHelper.showTitle(player, "<red>Usage", "<gray>/admincase givekey <crate> <player> <amount>");
            return true;
        }

        String crateName = args[1];
        String targetPlayerName = args[2];

        if (!plugin.getCrateManager().crateExists(crateName)) {
            ChatHelper.showTitle(player, "<red>Error", "<gray>Crate <gold>" + crateName + "<gray> not found");
            return true;
        }

        Crate crate = plugin.getCrateManager().getCrate(crateName);
        if (crate == null || crate.getKeyItem() == null) {
            ChatHelper.showTitle(player, "<red>Error", "<gray>Skrzynka <gold>" + crateName + "<gray> nie ma ustawionego klucza! Użyj /admincase setkey");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            ChatHelper.showTitle(player, "<red>Error", "<gray>Player <gold>" + targetPlayerName + "<gray> not found");
            return true;
        }

        try {
            int amount = Math.min(Integer.parseInt(args[3]), MAX_KEYS_PER_COMMAND);
            ItemStack keyItem = crate.getKeyItem().clone();
            keyItem.setAmount(amount);

            // Give physical keys to player
            targetPlayer.getInventory().addItem(keyItem).values().forEach(excess ->
                targetPlayer.getWorld().dropItem(targetPlayer.getLocation(), excess));

            ChatHelper.showTitle(player, "<green>Success", "<gray>Gave <gold>" + amount + "<gray> keys for <gold>" + crateName + "<gray> to <gold>" + targetPlayerName);
            ChatHelper.showTitle(targetPlayer, "<green>Keys Received", "<gray>You received <gold>" + amount + "<gray> keys for <gold>" + crateName);
        } catch (NumberFormatException e) {
            ChatHelper.showTitle(player, "<red>Error", "<gray>Invalid number: " + args[3]);
        }

        return true;
    }

    private boolean handleSetCase(Player player, String[] args) {
        if (args.length < 2) {
            ChatHelper.showTitle(player, "<red>Usage", "<gray>/admincase setcase <name>");
            return true;
        }

        String crateName = args[1];

        if (!plugin.getCrateManager().crateExists(crateName)) {
            ChatHelper.showTitle(player, "<red>Error", "<gray>Crate <gold>" + crateName + "<gray> not found");
            return true;
        }

        Block targetBlock = player.getTargetBlockExact(5);

        if (targetBlock == null || targetBlock.getType() == Material.AIR) {
            ChatHelper.showTitle(player, "<red>Error", "<gray>Look at a block to set the crate");
            return true;
        }

        plugin.getCrateManager().setCrateBlock(targetBlock, crateName);
        ChatHelper.showTitle(player, "<green>Success", "<gray>Block set as crate <gold>" + crateName);

        return true;
    }

    private boolean handleReload(Player player) {
        plugin.getCrateManager().reload();
        ChatHelper.showTitle(player, "<green>Success", "<gray>Plugin reloaded successfully");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("simplecase.admin")) {
            return new ArrayList<>();
        }

        return switch (args.length) {
            case 1 -> SUBCOMMANDS.stream()
                    .filter(sub -> sub.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
            case 2 -> {
                if (CRATE_REQUIRING_COMMANDS.contains(args[0].toLowerCase())) {
                    yield plugin.getCrateManager().getCrateNames().stream()
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .toList();
                }
                yield new ArrayList<>();
            }
            case 3 -> {
                if ("givekey".equalsIgnoreCase(args[0])) {
                    yield Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                            .toList();
                }
                yield new ArrayList<>();
            }
            default -> new ArrayList<>();
        };
    }
}
