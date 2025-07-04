package de.safti.saftiSk;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import de.safti.saftiSk.commands.MainCommand;
import de.safti.saftiSk.skript.dependencymanager.DownloadedDependency;
import de.safti.saftiSk.skript.dependencymanager.DependencyManager;
import de.safti.saftiSk.skript.elements.types.TypeLoader;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class SaftiSk extends JavaPlugin {
	private static final Logger log = LoggerFactory.getLogger(SaftiSk.class);
	public static SaftiSk INSTANCE;
	public static SkriptAddon addon;
	public static ConfigManager configManager;
	public static DependencyManager dependencyManager;
	
	private void loadSkriptFeatures() {
		try {
			addon.loadClasses("de.safti.saftiSk.skript", "elements");
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onEnable() {
		if(INSTANCE != null) {
			throw new IllegalStateException("Plugin already instantiated");
		}
		INSTANCE = this;
		
		// place config.yml in plugin folder if not present
		saveDefaultConfig();
		
		// init the config
		configManager = ConfigManager.init();
		
		// init skript addon
		addon = Skript.registerAddon(this);
		TypeLoader.init();
		loadSkriptFeatures();
		
		// start the dependency manager
		dependencyManager = DependencyManager.create(configManager);
		startDependencyManager();
		
		// load the SaftiSk command
		getCommand("saftisk").setExecutor(new MainCommand());
		
	}
	
	private void startDependencyManager() {
		if(dependencyManager == null) {
			log.error("Could not start dependency manager!");
			return;
		}
		
		CompletableFuture<Set<DownloadedDependency>> future = dependencyManager.downloadMissingDependencies();
		// ran on the main thread
		future.thenAcceptAsync(downloadedDependencies -> {
			dependencyManager.createProxyScripts(downloadedDependencies);
			dependencyManager.reloadAllScripts();
			
		}, Bukkit.getScheduler().getMainThreadExecutor(this));
		
		
		
	}
	
	@Override
	public void onDisable() {
		// Plugin shutdown logic
	}
	
}
