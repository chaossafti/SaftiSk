package de.safti.saftiSk;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import de.safti.saftiSk.skript.elements.types.TypeLoader;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public final class SaftiSk extends JavaPlugin {
    public static SaftiSk INSTANCE;
    public static SkriptAddon addon;


    private void loadSkript() {
        try {
            addon.loadClasses("de.safti.saftiSk.skript", "elements");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onEnable() {
        if(INSTANCE != null) {
            throw new IllegalStateException("Plugin already instantiated");
        }

        INSTANCE = this;
        addon = Skript.registerAddon(this);
        TypeLoader.init();
        loadSkript();

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

}
