package pl.polardev.scase.helpers;

import org.bukkit.inventory.InventoryHolder;
import pl.polardev.scase.guis.CrateAnimationGUI;
import pl.polardev.scase.guis.CrateEditGUI;
import pl.polardev.scase.guis.CrateMainGUI;
import pl.polardev.scase.guis.CrateOpenGUI;

import java.util.Set;

public class GUIHelper {

    private static final Set<Class<? extends InventoryHolder>> PLUGIN_GUI_TYPES = Set.of(
        CrateMainGUI.class,
        CrateOpenGUI.class,
        CrateAnimationGUI.class,
        CrateEditGUI.class
    );

    private static final Set<Class<? extends InventoryHolder>> CLICKABLE_GUI_TYPES = Set.of(
        CrateMainGUI.class,
        CrateOpenGUI.class
    );

    private static final Set<Class<? extends InventoryHolder>> CLOSE_HANDLING_TYPES = Set.of(
        CrateEditGUI.class,
        CrateAnimationGUI.class
    );

    public static boolean isPluginGUI(InventoryHolder holder) {
        return holder != null && PLUGIN_GUI_TYPES.contains(holder.getClass());
    }

    public static boolean isClickableGUI(InventoryHolder holder) {
        return holder != null && CLICKABLE_GUI_TYPES.contains(holder.getClass());
    }

    public static boolean requiresCloseHandling(InventoryHolder holder) {
        return holder != null && CLOSE_HANDLING_TYPES.contains(holder.getClass());
    }

    public static boolean shouldCancelClick(InventoryHolder holder) {
        return isPluginGUI(holder) && !(holder instanceof CrateEditGUI);
    }

    public static void handleGUIClose(InventoryHolder holder) {
        switch (holder) {
            case CrateEditGUI gui -> gui.onClose();
            case CrateAnimationGUI gui -> gui.onClose();
            default -> { /* No special handling needed */ }
        }
    }

    public static void handleGUIClick(InventoryHolder holder, int slot, org.bukkit.inventory.ItemStack item, boolean isTopInventory) {
        if (!isTopInventory) return;

        switch (holder) {
            case CrateMainGUI gui -> gui.handleClick(slot, item);
            case CrateOpenGUI gui -> gui.handleClick(slot);
            default -> { /* No click handling needed */ }
        }
    }
}
