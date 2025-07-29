package pl.polardev.scase.helper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class ItemBuilder {
    private final ItemStack item;
    private final ItemMeta meta;
    private static final MiniMessage mm = MiniMessage.miniMessage();

    private ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    private ItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(material);
    }

    public static ItemBuilder of(ItemStack item) {
        return new ItemBuilder(item);
    }

    public ItemBuilder name(String name) {
        if (meta != null) {
            meta.displayName(mm.deserialize(name));
        }
        return this;
    }

    public ItemBuilder lore(String... lore) {
        return lore(Arrays.asList(lore));
    }

    public ItemBuilder lore(List<String> lore) {
        if (meta != null) {
            List<Component> components = lore.stream()
                    .map(mm::deserialize)
                    .toList();
            meta.lore(components);
        }
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }

    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }
}
