package pl.polardev.scase.helper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;

public class ChatHelper {
    private static final MiniMessage mm = MiniMessage.miniMessage();

    public static void showTitle(Player player, String title, String subtitle) {
        Component titleComponent = mm.deserialize(title);
        Component subtitleComponent = mm.deserialize(subtitle);

        Title titleObj = Title.title(
            titleComponent,
            subtitleComponent,
            Title.Times.times(
                Duration.ofMillis(500),
                Duration.ofSeconds(2),
                Duration.ofMillis(500)
            )
        );

        player.showTitle(titleObj);
    }

    public static void sendMessage(Player player, String message) {
        Component component = mm.deserialize(message);
        player.sendMessage(component);
    }

    public static Component deserialize(String text) {
        return mm.deserialize(text);
    }
}
