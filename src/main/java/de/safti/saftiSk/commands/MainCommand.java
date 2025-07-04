package de.safti.saftiSk.commands;


import de.safti.saftiSk.commands.subcommands.InfoSubCommand;
import de.safti.saftiSk.commands.subcommands.ReloadSubCommand;
import de.safti.saftiSk.commands.subcommands.UpdateDependencySubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;

public class MainCommand implements CommandExecutor {
	private final Map<String, AbstractSubCommand> subCommandMap = new HashMap<>();
	
	public MainCommand() {
		subCommandMap.put("info", new InfoSubCommand());
		subCommandMap.put("update", new UpdateDependencySubCommand());
		subCommandMap.put("reload", new ReloadSubCommand());
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		// fixme: info sub command displaying wrong amount of dependencies loaded (displays 0)
		// possibly not saving files that weren't just downloaded aren't being added to the hashmap
		
		if(args.length < 1) {
			sender.sendMessage("/saftisk <subcommand>");
			return true;
		}
		
		String subCommandLabel = args[0];
		AbstractSubCommand subCommand = subCommandMap.get(subCommandLabel);
		if(subCommand == null) {
			send(sender, "&cCannot find sub command: " + subCommandLabel);
			return true;
		}
		
		String[] subArgs = new String[args.length - 1];
		System.arraycopy(args, 1, subArgs, 0, args.length - 1);
		
		subCommand.execute(sender, subArgs);
		return true;
	}
	
	public void send(CommandSender sender, String message) {
		var deserializer = LegacyComponentSerializer.legacyAmpersand();
		Component deserialized = deserializer.deserialize(message);
		sender.sendMessage(deserialized);
	}
	
}
