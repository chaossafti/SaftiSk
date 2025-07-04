package de.safti.saftiSk.skript.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import de.safti.saftiSk.skript.elements.types.datastruct.DataStructure;
import de.safti.saftiSk.skript.elements.types.datastruct.ListFieldType;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExprModifyDataContainer extends SimpleExpression<Object> {
    static final String FIELD_NAME_PATTERN = "[a-zA-Z0-9_]+";
    private static final Logger log = LoggerFactory.getLogger(ExprModifyDataContainer.class);
    private String fieldName;
    private Expression<DataStructure> dataStructureExpression;

    static {
        Skript.registerExpression(ExprModifyDataContainer.class, Object.class, ExpressionType.SIMPLE, "field %string% of %datastruct%", "%datastruct%.<" + FIELD_NAME_PATTERN + ">");
    }

    @Override
    protected Object[] get(Event event) {
        DataStructure structure = dataStructureExpression.getSingle(event);
        if(structure == null || !structure.hasField(fieldName)) {
            return new Object[0];
        }


        // init list fields
        DataStructure.FieldDescriptor descriptor = structure.getDescriptor(fieldName);
        if(descriptor.plural() && structure.get(fieldName) == null) {
            ListFieldType listField = new ListFieldType();
            structure.setValue(fieldName, listField);
            return new Object[] {listField};
        }

        return new Object[] {structure.get(fieldName)};
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if(matchedPattern == 0) {
            Expression<String> fieldNameExpression = (Expression<String>) expressions[0];
            if(!(fieldNameExpression instanceof Literal<String> fieldNameliteral)) {
                Skript.error("field name must be literal! You cannot use variables.");
                return false;
            }

            fieldName = fieldNameliteral.getSingle();
            dataStructureExpression = (Expression<DataStructure>) expressions[1];
        } else {
            fieldName = parseResult.regexes.getFirst().group(0);
            dataStructureExpression = (Expression<DataStructure>) expressions[0];
        }

        return true;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<?> getReturnType() {
        return Object.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "modify data container";
    }

    @Override
    public  Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if(mode == Changer.ChangeMode.SET) return new Class[] {Object.class};
        if(mode == Changer.ChangeMode.DELETE) return new Class[] {Object.class};
        if(mode == Changer.ChangeMode.RESET) return new Class[] {Object.class};
        return null;
    }

    @Override
    public void change(Event event, Object[] delta, Changer.ChangeMode mode) {
        var structure = dataStructureExpression.getSingle(event);
        if(structure == null) return;

        switch (mode) {
            case SET -> structure.setValue(fieldName, delta);
            case RESET, DELETE -> structure.delValue(fieldName);
        }
    }
}
