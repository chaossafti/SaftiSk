package de.safti.saftiSk.commands;


import de.safti.saftiSk.commands.subcommands.InfoSubCommand;
import de.safti.saftiSk.commands.subcommands.ReloadSubCommand;
import de.safti.saftiSk.commands.subcommands.UpdateDependencySubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class MainCommand extends AbstractCommandBase {
	public MainCommand() {
		registerSubCommand(new InfoSubCommand(), "info");
		registerSubCommand(new UpdateDependencySubCommand(), "update");
		registerSubCommand(new ReloadSubCommand(), "reload");
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
		AbstractSubCommand subCommand = getSubCommand(subCommandLabel);
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
