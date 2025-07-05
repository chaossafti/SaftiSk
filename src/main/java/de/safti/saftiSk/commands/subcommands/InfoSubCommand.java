package de.safti.saftiSk.commands.subcommands;

import de.safti.saftiSk.SaftiSk;
import de.safti.saftiSk.commands.api.AbstractSubCommand;
import de.safti.saftiSk.skript.dependencymanager.DependencyManager;
import org.bukkit.command.CommandSender;

public class InfoSubCommand extends AbstractSubCommand {
	
	@Override
	public void execute(CommandSender sender, String[] args) {
		DependencyManager dependencyManager = SaftiSk.dependencyManager;
		
		send(sender, "----SaftiSk Info----");
		send(sender, "&b  - Loaded Dependencies: " + dependencyManager.getLoadedDependencies().size());
		
		
	}
	
}
