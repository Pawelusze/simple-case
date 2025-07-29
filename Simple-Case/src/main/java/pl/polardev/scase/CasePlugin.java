package pl.polardev.scase;

import org.bukkit.plugin.java.JavaPlugin;
import pl.polardev.scase.command.AdminCaseCommand;
import pl.polardev.scase.listener.CrateListener;
import pl.polardev.scase.listener.GUIListener;
import pl.polardev.scase.manager.AnimationManager;
import pl.polardev.scase.manager.CrateManager;
import pl.polardev.scase.manager.RateLimitManager;

public class CasePlugin extends JavaPlugin {
    private CrateManager crateManager;
    private AnimationManager animationManager;
    private RateLimitManager rateLimitManager;
    private GUIListener guiListener;

    @Override
    public void onEnable() {
        // Initialize managers
        this.crateManager = new CrateManager(this);
        this.animationManager = new AnimationManager(this);
        this.rateLimitManager = new RateLimitManager(this);
        this.guiListener = new GUIListener(this);

        AdminCaseCommand adminCommand = new AdminCaseCommand(this);
        getCommand("admincase").setExecutor(adminCommand);
        getCommand("admincase").setTabCompleter(adminCommand);

        getServer().getPluginManager().registerEvents(new CrateListener(this), this);
        getServer().getPluginManager().registerEvents(guiListener, this);

        getLogger().info("Simple-Case plugin enabled successfully!");
        getLogger().info("Enterprise-level optimizations active - ready for 1000+ players");
    }

    @Override
    public void onDisable() {
        if (rateLimitManager != null) {
            rateLimitManager.shutdown();
        }
        if (animationManager != null) {
            animationManager.shutdown();
        }
        if (crateManager != null) {
            crateManager.saveAllCrates();
        }
        getLogger().info("Simple-Case plugin disabled successfully!");
    }

    public CrateManager getCrateManager() {
        return crateManager;
    }

    public AnimationManager getAnimationManager() {
        return animationManager;
    }

    public RateLimitManager getRateLimitManager() {
        return rateLimitManager;
    }

    public GUIListener getGUIListener() {
        return guiListener;
    }
}
