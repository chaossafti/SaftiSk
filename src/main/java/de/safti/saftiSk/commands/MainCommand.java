package de.safti.saftiSk.commands;


import de.safti.saftiSk.commands.api.AbstractCommandBase;
import de.safti.saftiSk.commands.subcommands.InfoSubCommand;
import de.safti.saftiSk.commands.subcommands.ReloadSubCommand;
import de.safti.saftiSk.commands.subcommands.UpdateDependencySubCommand;
import de.safti.saftiSk.utils.MessagingUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class MainCommand extends AbstractCommandBase {
	public MainCommand() {
		registerSubCommand(new InfoSubCommand(), "info");
		registerSubCommand(new UpdateDependencySubCommand(), "update");
		registerSubCommand(new ReloadSubCommand(), "reload");
	}
	
	@Override
	protected boolean requiresTabCompletions() {
		return true;
	}
	
	@Override
	protected void onInvalidCommandExecution(CommandSender sender, Command command, String label, String[] args) {
		send(sender, "&7[&6SaftiSk&7] &cWrong Syntax: ");
		MessagingUtils.builder()
				.recipient(sender)
				.appendInline("&7[&6SaftiSk&7] &7-  &c/saftiSk info")
				.hoverEvent("Displays info about the plugin")
				.send();
		
		MessagingUtils.builder()
				.recipient(sender)
				.appendInline("&7[&6SaftiSk&7] &7-  &c/saftiSk update <repository> <pathToFile>")
				.appendInline("\n&7[&6SaftiSk&7] &7-  &c/saftiSk update all")
				.hoverEvent("Downloads the newest version of the dependency.")
				.send();
		
		MessagingUtils.builder()
				.recipient(sender)
				.appendInline("&7[&6SaftiSk&7] &7-  &c/saftiSk reload")
				.hoverEvent("Reloads the config. Downloads missing Dependencies if new ones have been added.")
				.send();
		
	}
}
