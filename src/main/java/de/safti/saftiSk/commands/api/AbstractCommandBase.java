package de.safti.saftiSk.commands.api;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractCommandBase implements CommandExecutor, TabCompleter {
	private final Map<String, AbstractSubCommand> subCommandMap = new HashMap<>();
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(args.length < 1) {
			onInvalidCommandExecution(sender, command, label, args);
			return true;
		}
		
		String subCommandLabel = args[0];
		AbstractSubCommand subCommand = getSubCommand(subCommandLabel);
		if(subCommand == null) {
			onInvalidCommandExecution(sender, command, label, args);
			return true;
		}
		
		String[] subArgs = new String[args.length - 1];
		System.arraycopy(args, 1, subArgs, 0, args.length - 1);
		
		subCommand.execute(sender, subArgs);
		return true;
	}
	
	@Override
	public final List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		if(!requiresTabCompletions()) return List.of();
		
		if(args.length <= 1) return subCommandMap.keySet().stream().toList();
		
		String subCommandLabel = args[0];
		if(!subCommandMap.containsKey(subCommandLabel)) return List.of("invalid");
		
		AbstractSubCommand subCommand = getSubCommand(subCommandLabel);
		if(subCommand instanceof CommandTabCompleter commandTabCompleter) {
			String[] subArgs = new String[args.length - 1];
			System.arraycopy(args, 1, subArgs, 0, args.length - 1);
			
			return commandTabCompleter.get(sender, subArgs);
		}
		
		
		return List.of();
	}
	
	public void send(CommandSender sender, String message) {
		var deserializer = LegacyComponentSerializer.legacyAmpersand();
		Component deserialized = deserializer.deserialize(message);
		sender.sendMessage(deserialized);
	}
	
	protected void registerSubCommand(AbstractSubCommand subCommand, String... names) {
		if(subCommand == null) return;
		
		for (String name : names) {
			subCommandMap.put(name, subCommand);
		}
	}
	
	public AbstractSubCommand getSubCommand(String label) {
		return subCommandMap.get(label);
	}
	
	protected Set<String> subCommandLabels() {
		return subCommandMap.keySet();
	}
	
	protected abstract boolean requiresTabCompletions();
	
	protected abstract void onInvalidCommandExecution(CommandSender sender, Command command, String label, String[] args);
	
}
