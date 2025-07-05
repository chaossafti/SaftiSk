package de.safti.saftiSk.commands.subcommands;

import de.safti.saftiSk.SaftiSk;
import de.safti.saftiSk.commands.api.AbstractSubCommand;
import de.safti.saftiSk.skript.dependencymanager.DownloadedDependency;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ReloadSubCommand extends AbstractSubCommand {
	@Override
	public void execute(CommandSender sender, String[] args) {
		
		SaftiSk.configManager.reload();
		SaftiSk.dependencyManager.reload();
		
		
		CompletableFuture<Set<DownloadedDependency>> future = SaftiSk.dependencyManager.downloadMissingDependencies();
		// ran on the main thread
		future.thenAcceptAsync(downloadedDependencies -> {
			SaftiSk.dependencyManager.createProxyScripts(downloadedDependencies);
			SaftiSk.dependencyManager.reloadAllScripts();
			
		}, Bukkit.getScheduler().getMainThreadExecutor(SaftiSk.INSTANCE));
		
	}
	
}
