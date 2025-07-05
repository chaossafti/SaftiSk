package de.safti.saftiSk.commands;

import org.bukkit.command.CommandExecutor;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractCommandBase implements CommandExecutor {
	private final Map<String, AbstractSubCommand> subCommandMap = new HashMap<>();
	
	protected void registerSubCommand(AbstractSubCommand subCommand, String... names) {
		if(subCommand == null) return;
		
		for (String name : names) {
			subCommandMap.put(name, subCommand);
		}
	}
	
	public AbstractSubCommand getSubCommand(String label) {
		return subCommandMap.get(label);
	}
	

}
