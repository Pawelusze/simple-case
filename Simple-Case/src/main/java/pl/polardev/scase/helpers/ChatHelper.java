package pl.polardev.scase.helpers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.List;

public class ChatHelper {
    private static final MiniMessage mm = MiniMessage.miniMessage();

    public static Component deserialize(String message) {
        return mm.deserialize(message);
    }

    public static List<Component> deserializeList(List<String> messages) {
        return messages.stream()
                .map(ChatHelper::deserialize)
                .toList();
    }

    public static void sendMessage(Player player, String message) {
        player.sendMessage(deserialize(message));
    }

    public static void sendMessages(Player player, List<String> messages) {
        messages.forEach(message -> sendMessage(player, message));
    }

    public static void showTitle(Player player, String title, String subtitle) {
        showTitle(player, title, subtitle, Duration.ofMillis(500), Duration.ofSeconds(2), Duration.ofMillis(500));
    }

    public static void showTitle(Player player, String title, String subtitle, Duration fadeIn, Duration stay, Duration fadeOut) {
        player.showTitle(Title.title(
            deserialize(title),
            deserialize(subtitle),
            Title.Times.times(fadeIn, stay, fadeOut)
        ));
    }

    public static void showActionBar(Player player, String message) {
        player.sendActionBar(deserialize(message));
    }

    public static String stripTags(String message) {
        return mm.stripTags(message);
    }
}
