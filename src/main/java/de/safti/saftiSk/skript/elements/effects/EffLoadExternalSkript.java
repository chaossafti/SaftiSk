package de.safti.saftiSk.skript.elements.effects;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import de.safti.saftiSk.skript.logHandler.SaftiLogHandler;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.script.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class EffLoadExternalSkript extends Effect {
	private static final Logger log = LoggerFactory.getLogger(EffLoadExternalSkript.class);
	private Expression<String> scriptPath;
	private boolean reload;
	private boolean unload;
	
	static {
		Skript.registerEffect(EffLoadExternalSkript.class, "[1¦re]load external s(k|c)ript at %string%");
	}
	
	@Override
	protected void execute(Event event) {
		// ensure non-null path
		String scriptPath = this.scriptPath.getSingle(event);
		if(scriptPath == null) {
			return;
		}
		
		// ensure file exists
		File file = new File(scriptPath);
		if(!file.exists()) {
			log.warn("Tried loading external script {}, but no file was found.", scriptPath);
			return;
		}
		
		
		// file is not a plain file or normal directory
		Set<Script> scripts;
		if(file.isDirectory()) {
			scripts = ScriptLoader.getScripts(file);
		}
		else if(file.isFile()) {
			scripts = new HashSet<>();
			Script script = ScriptLoader.getScript(file);
			if(script != null) scripts.add(script);
		}
		else {
			log.warn("Something that isn't a file or directory was passed into the load external script effect!");
			return;
		}
		
		if(!scripts.isEmpty()) {
			if(unload) {
				ScriptLoader.unloadScripts(scripts);
				return;
				
			}
			
			if(!reload) {
				log.warn("Reloading was disabled and an already loaded skript was provided! Script: {}", scriptPath);
				return;
			}
			
			ScriptLoader.unloadScripts(scripts);
		}
		
		try (SaftiLogHandler logHandler = new SaftiLogHandler()) {
			ScriptLoader.loadScripts(file, logHandler)
					.thenAccept(scriptInfo -> {
						logHandler.printLog();
						
					});
		}
		
	}
	
	
	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "load external skript";
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		scriptPath = (Expression<String>) expressions[0];
		reload = parseResult.mark == 1;
		unload = matchedPattern == 1;
		return true;
	}
}
