package de.safti.saftiSk.commands.api;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface CommandTabCompleter {
	
	List<String> get(CommandSender sender, String[] args);
	
}
