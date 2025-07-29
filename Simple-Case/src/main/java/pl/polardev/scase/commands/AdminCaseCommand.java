package pl.polardev.scase.commands;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.polardev.scase.CasePlugin;
import pl.polardev.scase.guis.CrateEditGUI;
import pl.polardev.scase.models.Crate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AdminCaseCommand implements TabExecutor {
    private final CasePlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private static final List<String> SUBCOMMANDS = Arrays.asList("create", "edit", "delete", "setkey", "givekey");

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
            showTitle(player, "<red>No Permission", "<gray>You don't have permission to use this command");
            return true;
        }

        if (args.length == 0) {
            showTitle(player, "<gold>Usage", "<gray>/admincase {create|edit|delete|setkey|givekey}");
            return true;
        }

        String subcommand = args[0].toLowerCase();
        switch (subcommand) {
            case "create" -> handleCreate(player, args);
            case "edit" -> handleEdit(player, args);
            case "delete" -> handleDelete(player, args);
            case "setkey" -> handleSetKey(player, args);
            case "givekey" -> handleGiveKey(player, args);
            default -> showTitle(player, "<red>Invalid Command", "<gray>Use /admincase {create|edit|delete|setkey|givekey}");
        }

        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            showTitle(player, "<red>Usage", "<gray>/admincase create <name>");
            return;
        }

        String name = args[1];

        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || targetBlock.getType() == Material.AIR) {
            showTitle(player, "<red>Error", "<gray>Look at a block to create a crate");
            return;
        }

        if (plugin.getCrateManager().getCrate(name) != null) {
            showTitle(player, "<red>Error", "<gray>Crate with this name already exists");
            return;
        }

        plugin.getCrateManager().createCrate(name, targetBlock);
        showTitle(player, "<green>Success", "<gray>Crate <gold>" + name + "<gray> created successfully");
    }

    private void handleEdit(Player player, String[] args) {
        if (args.length < 2) {
            showTitle(player, "<red>Usage", "<gray>/admincase edit <name>");
            return;
        }

        String name = args[1];
        Crate crate = plugin.getCrateManager().getCrate(name);

        if (crate == null) {
            showTitle(player, "<red>Error", "<gray>Crate <gold>" + name + "<gray> not found");
            return;
        }

        new CrateEditGUI(plugin, player, crate).open();
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            showTitle(player, "<red>Usage", "<gray>/admincase delete <name>");
            return;
        }

        String name = args[1];
        Crate crate = plugin.getCrateManager().getCrate(name);

        if (crate == null) {
            showTitle(player, "<red>Error", "<gray>Crate <gold>" + name + "<gray> not found");
            return;
        }

        plugin.getCrateManager().deleteCrate(name);
        showTitle(player, "<green>Success", "<gray>Crate <gold>" + name + "<gray> deleted successfully");
    }

    private void handleSetKey(Player player, String[] args) {
        if (args.length < 2) {
            showTitle(player, "<red>Usage", "<gray>/admincase setkey <name>");
            return;
        }

        String name = args[1];
        Crate crate = plugin.getCrateManager().getCrate(name);

        if (crate == null) {
            showTitle(player, "<red>Error", "<gray>Crate <gold>" + name + "<gray> not found");
            return;
        }

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType() == Material.AIR) {
            showTitle(player, "<red>Error", "<gray>Hold an item in your hand to set as key");
            return;
        }

        crate.setKeyItem(heldItem);
        // Użyj natychmiastowego zapisu zamiast asynchronicznego
        plugin.getCrateManager().saveCrateImmediately(crate);
        showTitle(player, "<green>Success", "<gray>Key set for crate <gold>" + name);
    }

    private void handleGiveKey(Player player, String[] args) {
        if (args.length < 3) {
            showTitle(player, "<red>Usage", "<gray>/admincase givekey <crate> <player/all> [amount]");
            return;
        }

        String crateName = args[1];
        String target = args[2];
        int amount = args.length > 3 ? parseAmount(args[3]) : 1;

        Crate crate = plugin.getCrateManager().getCrate(crateName);
        if (crate == null) {
            showTitle(player, "<red>Error", "<gray>Crate <gold>" + crateName + "<gray> not found");
            return;
        }

        ItemStack keyItem = crate.getKeyItem();
        if (keyItem == null) {
            showTitle(player, "<red>Error", "<gray>Crate <gold>" + crateName + "<gray> has no key set");
            return;
        }

        if (target.equalsIgnoreCase("all")) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                giveKeysToPlayer(onlinePlayer, keyItem, amount);
            }
            showTitle(player, "<green>Success", "<gray>Gave <gold>" + amount + "<gray> keys to all players");
        } else {
            Player targetPlayer = Bukkit.getPlayer(target);
            if (targetPlayer == null) {
                showTitle(player, "<red>Error", "<gray>Player <gold>" + target + "<gray> not found");
                return;
            }

            giveKeysToPlayer(targetPlayer, keyItem, amount);
            showTitle(player, "<green>Success", "<gray>Gave <gold>" + amount + "<gray> keys to <gold>" + targetPlayer.getName());
        }
    }

    private void giveKeysToPlayer(Player player, ItemStack keyItem, int totalAmount) {
        int remaining = totalAmount;
        int maxStackSize = keyItem.getMaxStackSize();

        while (remaining > 0) {
            int stackSize = Math.min(remaining, maxStackSize);
            ItemStack key = keyItem.clone();
            key.setAmount(stackSize);

            var leftover = player.getInventory().addItem(key);
            if (!leftover.isEmpty()) {
                for (ItemStack leftoverItem : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftoverItem);
                }
            }

            remaining -= stackSize;
        }
    }

    private int parseAmount(String str) {
        try {
            int amount = Integer.parseInt(str);
            return Math.max(1, Math.min(amount, 10000)); // Max 10000 kluczy
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private void showTitle(Player player, String title, String subtitle) {
        player.showTitle(Title.title(
            mm.deserialize(title),
            mm.deserialize(subtitle),
            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
        ));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("simplecase.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(sub -> sub.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subcommand = args[0].toLowerCase();
            if (Arrays.asList("edit", "delete", "setkey", "givekey").contains(subcommand)) {
                return plugin.getCrateManager().getCrateNames().stream()
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("givekey")) {
            List<String> players = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
            if ("all".startsWith(args[2].toLowerCase())) {
                players.add("all");
            }
            return players;
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("givekey")) {
            return Arrays.asList("1", "5", "10", "16", "32", "64", "100", "500", "1000");
        }

        return new ArrayList<>();
    }
}
