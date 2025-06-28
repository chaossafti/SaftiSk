package de.safti.saftiSk.skript.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import de.safti.saftiSk.skript.elements.types.datastruct.DataStructure;
import de.safti.saftiSk.skript.elements.types.datastruct.ListFieldType;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@SuppressWarnings("removal")
@Name("Create data Structure")
@Description(
        "Creates a new Data Container with provided Values. Values are given as a list variable"
)
@Examples({
        "data structure \"player\"",
        "\tname: string",
        "",
        "set {_values::name} to player's name",
        "set {_struct} to a new data structure player with values {_values::*}"
})
@Since("1.0.0")
public class ExprCreateDataContainer extends SimpleExpression<DataStructure> {
    private static final Logger log = LoggerFactory.getLogger(ExprCreateDataContainer.class);
    private String name;
    private Variable<?> variable;

    static {
        Skript.registerExpression(ExprCreateDataContainer.class, DataStructure.class, ExpressionType.SIMPLE, "[a] [new] [data] struct[ure] <.+> with values %objects%");
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        Expression<?> var = defendExpression(expressions[0]);
        if(!(var instanceof Variable) || !((Variable<?>) var).isList()) {
            Skript.error(var + " is not a list variable.");
            return false;
        }

        variable = (Variable<?>) var;
        name = parseResult.regexes.getFirst().group(0).trim();
        if(!DataStructure.exists(name)) {
            Skript.error("No data structure named " + name + " was found!");
            return false;
        }
        return true;
    }

    @Override
    protected DataStructure @Nullable [] get(Event event) {
        DataStructure struct = DataStructure.fromName(name);
        if(struct == null) {
            log.warn("Could not find data structure with name {} at runtime", name);
            return new DataStructure[0];
        }


        //noinspection unchecked
        Map<String, Object> rawVariable = (Map<String, Object>) variable.getRaw(event);
        if(rawVariable == null) return new DataStructure[]{struct};

        for (String index : rawVariable.keySet()) {
            Object o = rawVariable.get(index);

            if(!struct.hasField(index)) {
                log.error("Tried setting field %s, but structure %s does not have that field!".formatted(index, struct.getStructureName()));
                continue;
            }
            DataStructure.FieldDescriptor descriptor = struct.getDescriptor(index);


            if(o instanceof Map<?, ?> subVariable) {
                if(!descriptor.plural()) {
                    log.error("tried assigning multiple values to single list field value");
                    continue;
                }

                ListFieldType listField = new ListFieldType();
                struct.setValue(index, listField);

                // fill the list field with values
                fillListField(listField, (Map<String, Object>) subVariable);
                continue;
            }

            if(!descriptor.clazz().isInstance(o)) {
                log.error("Tried setting field %s%s, but object (%s) has the wrong type! (Expected: %s)".formatted(struct.getStructureName(), index, o.getClass(), descriptor.clazz()));
                continue;
            }
            struct.setValue(index, o);
        }


        return new DataStructure[] {struct};
    }

    // TODO: add visited set if this errors
    private void fillListField(ListFieldType listField, Map<String, Object> rawVariable) {
        for (String index : rawVariable.keySet()) {
            Object o = rawVariable.get(index);

            if(o instanceof Map subVariabe) {
                fillListField(listField, subVariabe);
            }

            listField.put(index, o);
        }

    }

    @SuppressWarnings("unchecked")
    public static <T> Expression<T> defendExpression(Expression<?> expr) {
        if(expr instanceof UnparsedLiteral) {
            Literal<?> parsed = ((UnparsedLiteral) expr).getConvertedExpression(Object.class);
            return (Expression<T>) (parsed == null ? expr : parsed);
        } else if(expr instanceof ExpressionList) {
            Expression<?>[] exprs = ((ExpressionList<?>) expr).getExpressions();
            for (int i = 0; i < exprs.length; i++) {
                exprs[i] = defendExpression(exprs[i]);
            }
        }
        return (Expression<T>) expr;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends DataStructure> getReturnType() {
        return DataStructure.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "new data structure " + name;
    }
}
