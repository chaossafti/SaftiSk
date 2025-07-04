package de.safti.saftiSk.skript.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import de.safti.saftiSk.skript.elements.types.datastruct.ListFieldNode;
import de.safti.saftiSk.skript.elements.types.datastruct.ListFieldType;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ExprListFieldValues extends SimpleExpression<Object> {
    private static final Logger log = LoggerFactory.getLogger(ExprListFieldValues.class);
    private boolean single;
    private Expression<String> pathExpression;
    private Expression<ListFieldType> dataStructureExpression;


    static {
        // who named this datastructurelistfield??? Oh, that was me.
        Skript.registerExpression(ExprListFieldValues.class, Object.class, ExpressionType.SIMPLE, "value[1¦s] [at] %string% in %datastructurelistfield%");
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        single = parseResult.mark != 1;
        pathExpression = (Expression<String>) expressions[0];
        dataStructureExpression = (Expression<ListFieldType>) expressions[1];
        return true;
    }

    @Override
    protected Object[] get(Event event) {
        // get expressions and null check
        String path = pathExpression.getSingle(event);
        ListFieldType listField = dataStructureExpression.getSingle(event);
        if(path == null || listField == null) return new Object[0];

        if(!isValidPath(path)) return new Object[0];

        // check for multiple values
        if(path.endsWith("*")) {
            // return multiple values
            return listField.getMultiples(path);
        }

        // return only a single value
        return new Object[]{listField.get(path)};
    }

    @Override
    public void change(Event event, Object[] delta, Changer.ChangeMode mode) {
        // get expressions and null check
        String path = pathExpression.getSingle(event);
        ListFieldType listField = dataStructureExpression.getSingle(event);
        if(path == null || listField == null) return;
        if(!isValidPath(path)) return;

        boolean isSingle = !path.endsWith("*");

        switch (mode) {
            case ADD -> add(listField, path, delta, isSingle);
            case SET -> set(listField, path, delta, isSingle);
            case REMOVE -> remove(listField, path, delta, isSingle);
            case DELETE, REMOVE_ALL, RESET -> delete(listField, path, isSingle);
        }
    }

    private void add(ListFieldType listField, String path, Object[] delta, boolean requestsSingularPathValue) {
        log.info("add");
        if(delta == null) return;

        if(requestsSingularPathValue) {
            if(delta.length > 1) return;
            if(!listField.containsKey(path)) return;

            Object v = listField.get(path);

            // add the numbers together
            if(delta[0] instanceof Number n && v instanceof Number other) {
                listField.put(path, n.doubleValue() + other.doubleValue());
                return;
            }

            // unable to add anything
            return;
        }

        // add the object(s) to the parent of path
        addAllToList(listField, path, delta);
    }

    private void addAllToList(ListFieldType listField, String path, Object[] values) {
        // add the object(s) to the parent of path
        ListFieldNode parentNode = listField.lookupParent(path);
        Set<String> childNames = parentNode.children().keySet();

        int nextName = childNames.size();
        for (Object o : values) {
            if(o == null) continue;

            // make sure the child doesn't already exist
            if(childNames.contains(nextName + "")) {
                int attempt = 0;
                while(attempt < 100) {
                    attempt++;
                    nextName++;
                    if(childNames.contains(nextName + "")) {
                        continue;
                    }
                    break;
                }
            }

            // make sure we found a fitting name within n attempts
            if(childNames.contains(nextName + "")) {
                log.error("Could not add value: {} (Class: {}) in a list field; Could not find find fitting name for value within 100 attempts", o, o.getClass());
                continue;
            }

            // finally, add the value
            parentNode.putChild(nextName + "", o);
        }
    }

    private void set(ListFieldType listField, String path, Object[] delta, boolean requestsSingularPathValue) {
        if(delta == null) {
            delete(listField, path, requestsSingularPathValue);
            return;
        }

        if(requestsSingularPathValue) {
            listField.put(path, delta[0]);
            return;
        }

        ListFieldNode parentNode = listField.lookupParent(path);
        parentNode.children().clear();
        addAllToList(listField, path, delta);
    }

    private void remove(ListFieldType listField, String path, Object[] toRemove, boolean requestsSingularPathValue) {
        if(requestsSingularPathValue) {
            listField.remove(path);
            return;
        }

        // convert to set for blazingly fast access!
        Set<Object> toRemoveSet = new HashSet<>(Arrays.asList(toRemove));
        ListFieldNode parent = listField.lookupParent(path);
        Set<String> childNamesToRemove = new HashSet<>();
        // essentially devils list

        for (String childName : parent.children().keySet()) {
            ListFieldNode child = parent.children().get(childName);
            if(toRemoveSet.contains(child.value())) {
                childNamesToRemove.add(childName);
            }
        }

        // remove all children
        for (String name : childNamesToRemove) {
            parent.children().remove(name);
        }

    }

    private void delete(ListFieldType listField, String path, boolean requestsSingularPathValue) {
        if(requestsSingularPathValue) {
            listField.remove(path);
            return;
        }

        ListFieldNode parent = listField.lookupParent(path);
        parent.children().clear();
    }

    private boolean isValidPath(String path) {
        // validate path
        if(!ListFieldType.isValidPath(path)) {
            log.warn("got invalid path: {}", path);
            return false;
        }

        // make sure we do only return one value when the code asks for one
        if(single && path.endsWith("*")) {
            log.warn("Path tried returning multiple values when only asking for one (path: {})", path);
            return false;
        }

        return true;
    }

    @Override
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET -> single ? new Class[] {Object.class} : new Class[] {Object[].class};
            case RESET, DELETE, REMOVE_ALL -> new Class[]{Object.class};
            case ADD, REMOVE -> {
                if(single) yield new Class[]{Number.class};
                yield new Class[]{Object[].class};
            }
        };
    }

    @Override
    public boolean isSingle() {
        return single;
    }

    @Override
    public Class<?> getReturnType() {
        return Object.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "values of list field";
    }
}
