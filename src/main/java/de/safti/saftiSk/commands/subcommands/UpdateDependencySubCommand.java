package de.safti.saftiSk.commands.subcommands;

import ch.njol.skript.ScriptLoader;
import de.safti.saftiSk.ConfigManager;
import de.safti.saftiSk.SaftiSk;
import de.safti.saftiSk.commands.AbstractSubCommand;
import de.safti.saftiSk.skript.dependencymanager.DependencyManager;
import de.safti.saftiSk.skript.dependencymanager.DownloadedDependency;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.skriptlang.skript.lang.script.Script;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class UpdateDependencySubCommand extends AbstractSubCommand {
	
	@Override
	public void execute(CommandSender sender, String[] args) {
		if(args.length != 2) {
			send(sender,"&cWrong syntax! Expected: /saftisk update <repo> <path-to-file>");
			return;
		}
		
		String repo =  args[0];
		String path = args[1];
		ConfigManager configManager = SaftiSk.configManager;
		DependencyManager dependencyManager = SaftiSk.dependencyManager;
		File proxyFile = dependencyManager.getProxyFile(repo, path, false);

		
		// make sure dependency is registered
		if(!configManager.hasDependencyRegistered(repo, path)) {
			send(sender, "&cNot found! To update the Dependency, it must be registered in the Config!");
			return;
		}
		
		// redownload everything
		if(path.equals("all")) {
			CompletableFuture<Set<DownloadedDependency>> dependenciesFuture = dependencyManager.downloadAll();
			dependenciesFuture.thenAccept(downloadedDependencies -> {
				dependencyManager.createProxyScripts(downloadedDependencies);
				dependencyManager.reloadAllScripts();
			});
			
			
			return;
		}
		
		// download a single dependency
		CompletableFuture<DownloadedDependency> dependencyFuture = dependencyManager.download(repo, path);
		
		dependencyFuture.thenAccept(downloadedDependency -> {
			// unload the already loaded dependencies
			Set<Script> scripts = dependencyManager.getLoadedDependencies()
					.stream()
					.filter(script -> script.getConfig().getFile().equals(proxyFile))
					.collect(Collectors.toUnmodifiableSet());
			
			ScriptLoader.unloadScripts(scripts);
			
			try {
				// make sure the proxy file exists
				dependencyManager.writeProxyFile(downloadedDependency);
				
				// load the script
				dependencyManager.load(repo, downloadedDependency.filePath());
				Bukkit.getScheduler().runTask(SaftiSk.INSTANCE, () -> send(sender, "&aSuccessfully updated %s/%s".formatted(repo, downloadedDependency.filePath())));
				
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
			
		});
		
	}
	
	private void load(DownloadedDependency dependency) {
	
	}
	
}
