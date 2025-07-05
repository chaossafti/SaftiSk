package de.safti.saftiSk.commands.api;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

public abstract class AbstractSubCommand {
	
	public abstract void execute(CommandSender sender, String[] args);
	
	
	public void send(CommandSender sender, String message) {
		var deserializer = LegacyComponentSerializer.legacyAmpersand();
		Component deserialized = deserializer.deserialize(message);
		sender.sendMessage(deserialized);
	}
	
}
