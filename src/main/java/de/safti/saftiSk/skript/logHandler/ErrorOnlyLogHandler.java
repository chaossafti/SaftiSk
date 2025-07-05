package de.safti.saftiSk.skript.logHandler;

import ch.njol.skript.log.LogEntry;
import ch.njol.skript.log.LogHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.ServerOperator;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class ErrorOnlyLogHandler extends LogHandler {
	private final Collection<CommandSender> recipients;
	private final String errorInfo;
	private final Map<Level, Collection<LogEntry>> logs = new HashMap<>();
	private int numErrors;
	private int numWarns;
	private final long startTime;
	
	public static Collection<CommandSender> getDefaultRecipients() {
		Collection<CommandSender> recipients = Bukkit.getOnlinePlayers()
				.stream()
				.filter(ServerOperator::isOp)
				.collect(Collectors.toSet());
		
		recipients.add(Bukkit.getConsoleSender());
		return recipients;
	}
	
	public ErrorOnlyLogHandler(String errorInfo) {
		this(getDefaultRecipients(), errorInfo);
	}
	
	public ErrorOnlyLogHandler(Collection<CommandSender> recipients, String errorInfo) {
		this.recipients = recipients;
		this.errorInfo = errorInfo;
		startTime = System.currentTimeMillis();
	}
	
	@Override
	public LogResult log(LogEntry entry) {
		if(entry.level == Level.SEVERE) {
			numErrors++;
		}
		
		if(entry.level == Level.WARNING) {
			numWarns++;
		}
		
		logs.computeIfAbsent(entry.getLevel(), level -> new HashSet<>())
				.add(entry);
		return LogResult.CACHED;
	}
	
	@Override
	public void close() {
		super.close();
		
	}
	
	public void printLog() {
		if(numErrors < 0) {
			return;
		}
		
		long timeMillis = System.currentTimeMillis() - startTime;
		if(numErrors > 0) {
			send("&7[&aSaftiSk&7] &f" + errorInfo);
		}
		
		// log to the players
		for (Level level : logs.keySet()) {
			Collection<LogEntry> entries = logs.get(level);
			TextColor color = textColorFromLogLevel(level);
			
			TextComponent.Builder builder = Component.text();
			builder.content("  " + friendlyName(level) + ": ")
					.color(color)
					.decorate(TextDecoration.BOLD);
			
			boolean isFirst = true;
			for (LogEntry entry : entries) {
				builder.append(toTextComponent(entry, isFirst));
				isFirst = false;
			}
			
			// send component
			TextComponent component = builder.build();
			recipients.forEach(commandSender -> commandSender.sendMessage(component));
		}
		printStats(timeMillis);
	}
	
	private void printStats(long timeMillis) {
		// send to player
		if(numWarns == 0 && numErrors == 0) {
			send("&aNice code! You don't have any errors or warnings.");
			return;
		}
		
		send(" ");
		
		if(numErrors > 0) {
			send("&c  Encountered " + numErrors + " error(s) while reloading");
		}
		
		if(numWarns > 0) {
			send("&e  Encountered " + numWarns + " warning(s) while reloading");
		}
		
		send("&7  Took&6 " + timeMillis + "ms &7to reload");
	}
	
	private TextComponent toTextComponent(LogEntry entry, boolean isFirst) {
		TextComponent.Builder entryComponentBuilder = Component.text();
		String prefix = "";
		
		// prepend ", " if there's a value before this one
		if(!isFirst) {
			prefix = ", ";
		}
		
		
		// if the node is null, append unknown
		if(entry.node == null) {
			entryComponentBuilder.content(prefix + "unknown");
			return entryComponentBuilder.build();
		}
		
		entryComponentBuilder.content(prefix + entry.node.getLine())
				.color(textColorFromLogLevel(entry.getLevel()))
				.decorate(TextDecoration.BOLD);
		
		// hover to show an error message
		entryComponentBuilder.hoverEvent(HoverEvent.showText(Component.text(entry.toFormattedString())));
		return entryComponentBuilder.build();
	}
	
	
	@Override
	public LogHandler start() {
		
		return super.start();
	}
	
	public void send(String s) {
		var deserializer = LegacyComponentSerializer.legacyAmpersand();
		Component deserialized = deserializer.deserialize(s);
		recipients.forEach(commandSender -> commandSender.sendMessage(deserialized));
	}
	
	public String friendlyName(Level level) {
		if(level == Level.SEVERE) return "Error";
		if(level == Level.WARNING) return "Warning";
		return level.getName();
		
	}
	
	public TextColor textColorFromLogLevel(Level level) {
		if(level == Level.SEVERE) return NamedTextColor.RED;
		if(level == Level.WARNING) return NamedTextColor.YELLOW;
		return NamedTextColor.GRAY;
		
	}
	
	
}
