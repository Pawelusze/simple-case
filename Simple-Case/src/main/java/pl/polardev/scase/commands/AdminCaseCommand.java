package pl.polardev.scase.commands;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.polardev.scase.CasePlugin;
import pl.polardev.scase.guis.CrateEditGUI;
import pl.polardev.scase.helpers.ChatHelper;
import pl.polardev.scase.models.Crate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class AdminCaseCommand implements TabExecutor {
    private final CasePlugin plugin;
    private static final Set<String> SUBCOMMANDS = Set.of("create", "edit", "delete", "setkey", "givekey");
    private static final Set<String> CRATE_REQUIRING_COMMANDS = Set.of("edit", "delete", "setkey", "givekey");
    private static final int MAX_KEYS_PER_COMMAND = 10000;

    public AdminCaseCommand(CasePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("simplecase.admin")) {
            ChatHelper.showTitle(player, "<red>No Permission", "<gray>You don't have permission to use this command");
            return true;
        }

        if (args.length == 0) {
            ChatHelper.showTitle(player, "<gold>Usage", "<gray>/admincase {create|edit|delete|setkey|givekey}");
            return true;
        }

        final String subcommand = args[0].toLowerCase();
        
        return switch (subcommand) {
            case "create" -> handleCreate(player, args);
            case "edit" -> handleEdit(player, args);
            case "delete" -> handleDelete(player, args);
            case "setkey" -> handleSetKey(player, args);
            case "givekey" -> handleGiveKey(player, args);
            default -> {
                ChatHelper.showTitle(player, "<red>Invalid Command", "<gray>Use /admincase {create|edit|delete|setkey|givekey}");
                yield true;
            }
        };
    }

    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            ChatHelper.showTitle(player, "<red>Usage", "<gray>/admincase create <name>");
            return true;
        }

        final String name = args[1];
        final Optional<Block> targetBlock = Optional.ofNullable(player.getTargetBlockExact(5))
                .filter(block -> block.getType() != Material.AIR);

        if (targetBlock.isEmpty()) {
            ChatHelper.showTitle(player, "<red>Error", "<gray>Look at a block to create a crate");
            return true;
        }

        if (plugin.getCrateManager().getCrate(name) != null) {
            ChatHelper.showTitle(player, "<red>Error", "<gray>Crate with this name already exists");
            return true;
        }

        plugin.getCrateManager().createCrate(name, targetBlock.get());
        ChatHelper.showTitle(player, "<green>Success", "<gray>Crate <gold>" + name + "<gray> created successfully");
        return true;
    }

    private boolean handleEdit(Player player, String[] args) {
        return withValidCrate(player, args, "edit", crate -> {
            new CrateEditGUI(plugin, player, crate).open();
            return true;
        });
    }

    private boolean handleDelete(Player player, String[] args) {
        return withValidCrate(player, args, "delete", crate -> {
            plugin.getCrateManager().deleteCrate(crate.getName());
            ChatHelper.showTitle(player, "<green>Success", "<gray>Crate <gold>" + crate.getName() + "<gray> deleted successfully");
            return true;
        });
    }

    private boolean handleSetKey(Player player, String[] args) {
        final ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType() == Material.AIR) {
            ChatHelper.showTitle(player, "<red>Error", "<gray>Hold an item in your hand to set as key");
            return true;
        }

        return withValidCrate(player, args, "setkey", crate -> {
            crate.setKeyItem(heldItem);
            plugin.getCrateManager().saveCrateImmediately(crate);
            ChatHelper.showTitle(player, "<green>Success", "<gray>Key set for crate <gold>" + crate.getName());
            return true;
        });
    }

    private boolean handleGiveKey(Player player, String[] args) {
        if (args.length < 3) {
            ChatHelper.showTitle(player, "<red>Usage", "<gray>/admincase givekey <crate> <player/all> [amount]");
            return true;
        }

        final String crateName = args[1];
        final String target = args[2];
        final int amount = args.length > 3 ? parseAmount(args[3]) : 1;

        return withValidCrate(crateName, crate -> {
            return crate.getKeyItem()
                    .map(keyItem -> {
                        if (target.equalsIgnoreCase("all")) {
                            return giveKeysToAllPlayers(player, keyItem, amount);
                        } else {
                            return giveKeysToPlayer(player, target, keyItem, amount);
                        }
                    })
                    .orElseGet(() -> {
                        ChatHelper.showTitle(player, "<red>Error", "<gray>Crate <gold>" + crateName + "<gray> has no key set");
                        return true;
                    });
        });
    }

    private boolean withValidCrate(Player player, String[] args, String commandName, CrateFunction function) {
        if (args.length < 2) {
            ChatHelper.showTitle(player, "<red>Usage", "<gray>/admincase " + commandName + " <name>");
            return true;
        }
        return withValidCrate(args[1], function);
    }

    private boolean withValidCrate(String crateName, CrateFunction function) {
        final Crate crate = plugin.getCrateManager().getCrate(crateName);
        if (crate == null) {
            ChatHelper.showTitle(null, "<red>Error", "<gray>Crate <gold>" + crateName + "<gray> not found");
            return true;
        }
        return function.apply(crate);
    }

    private boolean giveKeysToAllPlayers(Player sender, ItemStack keyItem, int amount) {
        final var onlinePlayers = Bukkit.getOnlinePlayers();
        onlinePlayers.parallelStream()
                .forEach(player -> distributeKeys(player, keyItem, amount));

        ChatHelper.showTitle(sender, "<green>Success",
                "<gray>Gave <gold>" + amount + "<gray> keys to <gold>" + onlinePlayers.size() + "<gray> players");
        return true;
    }

    private boolean giveKeysToPlayer(Player sender, String targetName, ItemStack keyItem, int amount) {
        return Optional.ofNullable(Bukkit.getPlayer(targetName))
                .map(targetPlayer -> {
                    distributeKeys(targetPlayer, keyItem, amount);
                    ChatHelper.showTitle(sender, "<green>Success",
                            "<gray>Gave <gold>" + amount + "<gray> keys to <gold>" + targetPlayer.getName());
                    return true;
                })
                .orElseGet(() -> {
                    ChatHelper.showTitle(sender, "<red>Error", "<gray>Player <gold>" + targetName + "<gray> not found");
                    return true;
                });
    }

    private void distributeKeys(Player player, ItemStack keyItem, int totalAmount) {
        final int maxStackSize = keyItem.getMaxStackSize();

        Stream.iterate(totalAmount, remaining -> remaining > 0, remaining -> remaining - maxStackSize)
                .mapToInt(remaining -> Math.min(remaining, maxStackSize))
                .mapToObj(stackSize -> {
                    final ItemStack key = keyItem.clone();
                    key.setAmount(stackSize);
                    return key;
                })
                .forEach(key -> {
                    final var leftover = player.getInventory().addItem(key);
                    leftover.values().forEach(item ->
                            player.getWorld().dropItemNaturally(player.getLocation(), item));
                });
    }

    private int parseAmount(String str) {
        try {
            return Math.clamp(Integer.parseInt(str), 1, MAX_KEYS_PER_COMMAND);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("simplecase.admin")) {
            return List.of();
        }

        return switch (args.length) {
            case 1 -> getFilteredSubcommands(args[0]);
            case 2 -> getSecondArgumentCompletions(args[0], args[1]);
            case 3 -> getThirdArgumentCompletions(args[0], args[2]);
            default -> List.of();
        };
    }

    private List<String> getFilteredSubcommands(String input) {
        return SUBCOMMANDS.stream()
                .filter(sub -> sub.startsWith(input.toLowerCase()))
                .sorted()
                .toList();
    }

    private List<String> getSecondArgumentCompletions(String command, String input) {
        return CRATE_REQUIRING_COMMANDS.contains(command.toLowerCase())
                ? getFilteredCrateNames(input)
                : List.of();
    }

    private List<String> getThirdArgumentCompletions(String command, String input) {
        return "givekey".equalsIgnoreCase(command)
                ? getFilteredPlayerNames(input)
                : List.of();
    }

    private List<String> getFilteredCrateNames(String input) {
        return plugin.getCrateManager().getCrateNames().stream()
                .filter(name -> name.toLowerCase().startsWith(input.toLowerCase()))
                .sorted()
                .toList();
    }

    private List<String> getFilteredPlayerNames(String input) {
        final String lowerInput = input.toLowerCase();
        final List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(lowerInput))
                .sorted()
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        if ("all".startsWith(lowerInput)) {
            playerNames.add(0, "all"); // Add "all" at the beginning
        }

        return playerNames;
    }

    @FunctionalInterface
    private interface CrateFunction {
        boolean apply(Crate crate);
    }
}
