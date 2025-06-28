package de.safti.saftiSk.skript.elements.structures;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.log.LogEntry;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import de.safti.saftiSk.skript.elements.types.datastruct.DataStructure;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.structure.Structure;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class StructDataContainer extends Structure {
    private static final Priority PRIORITY = new Priority(350); // event run at priority 500; functions run at priority 400; run before either

    private static final String FIELD_NAME_PATTERN = "[a-zA-Z0-9_]+";
    private String structureName;
    private Map<String, DataStructure.FieldDescriptor> fields = new HashMap<>();
    private EntryContainer entryContainer;

    static {
        Skript.registerStructure(StructDataContainer.class, "data (struct[ure]|container) %string%");
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult, @Nullable EntryContainer entryContainer) {
        structureName = ((Literal<String>) args[0]).getSingle();
        this.entryContainer = entryContainer;
        return true;
    }

    @Override
    public boolean load() {
        for (var node : entryContainer.getUnhandledNodes()) {
            String key = node.getKey();
            if(key == null) continue;

            String[] split = key.split(":");
            if(split.length != 2) continue;

            String fieldName = split[0].trim();
            String type = split[1].trim();

            if(!fieldName.matches(FIELD_NAME_PATTERN)) {
                SkriptLogger.log(new LogEntry(Level.SEVERE, "Cannot understand field definition: " + key, node));
                continue;
            }


            ClassInfo<?> classInfo = Classes.getClassInfoFromUserInput(type);
            if(classInfo == null) {
                SkriptLogger.log(new LogEntry(Level.SEVERE, "Cannot understand type: " + type, node));
                continue;
            }

            boolean success = registerField(fieldName, classInfo);
            if(!success) {
                SkriptLogger.log(new LogEntry(Level.SEVERE, "Field named " + fieldName + " already exists!", node));
            }
        }

        DataStructure.updateFields(this.structureName, fields);
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "data structure " + structureName;
    }

    public boolean registerField(String fieldName, ClassInfo<?> type) {
        if(fields.containsKey(fieldName)) {
            return false;
        }

        DataStructure.FieldDescriptor descriptor = new DataStructure.FieldDescriptor(type.getC(), fieldName);
        fields.put(fieldName, descriptor);
        return true;
    }

    @Override
    public Priority getPriority() {
        return PRIORITY;
    }
}
