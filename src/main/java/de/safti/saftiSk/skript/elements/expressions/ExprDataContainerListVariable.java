package de.safti.saftiSk.skript.elements.expressions;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// NOT USED
public class ExprDataContainerListVariable extends SimpleExpression<Object> {
    private static final Logger log = LoggerFactory.getLogger(ExprDataContainerListVariable.class);
    private String path;

    static {
        // I won't even try explaining this regex.
        //Skript.registerExpression(ExprDataContainerListVariable.class, Object.class, ExpressionType.SIMPLE, "%datastruct%.<[a-zA-Z0-9_]+(::([a-zA-Z0-9_]+|%.+%))+>");
    }


    @Override
    protected Object @Nullable [] get(Event event) {
        return new Object[0];
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        path = parseResult.regexes.getFirst().group();
        String[] directions = path.split("::");
        for (int i = 0, directionsLength = directions.length; i < directionsLength; i++) {
            String direction = directions[i];

            // * spotted in the not last index
            if(direction.equals("*") && i != directionsLength - 1) {

            }

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
        return "list variable";
    }
}
