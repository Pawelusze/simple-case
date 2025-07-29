package pl.polardev.scase;

import org.bukkit.plugin.java.JavaPlugin;
import pl.polardev.scase.commands.AdminCaseCommand;
import pl.polardev.scase.listeners.CrateListener;
import pl.polardev.scase.listeners.GUIListener;
import pl.polardev.scase.managers.CrateManager;

public final class CasePlugin extends JavaPlugin {

    private CrateManager crateManager;

    @Override
    public void onEnable() {
        this.crateManager = new CrateManager(this);

        getCommand("admincase").setExecutor(new AdminCaseCommand(this));

        getServer().getPluginManager().registerEvents(new CrateListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
    }

    @Override
    public void onDisable() {
        if (crateManager != null) {
            crateManager.shutdown();
        }
    }

    public CrateManager getCrateManager() {
        return crateManager;
    }
}
