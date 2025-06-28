package de.safti.saftiSk.skript.elements.types.datastruct;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ListFieldNode {
    private Object value;
    private final Map<String, ListFieldNode> children;

    public ListFieldNode(Object value, Map<String, ListFieldNode> children) {
        this.value = value;
        this.children = children;
    }

    public ListFieldNode getChild(String name) {
        return children.get(name);
    }

    public ListFieldNode getOrMakeChild(String name) {
        return children.computeIfAbsent(name, s -> new ListFieldNode(null, new HashMap<>()));
    }

    public Object value() {
        return value;
    }

    public Map<String, ListFieldNode> children() {
        return children;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public ListFieldNode putChild(String childName, Object value) {
        ListFieldNode node = new ListFieldNode(value, new HashMap<>());
        children.put(childName, node);
        return node;
    }
    @Override
    public boolean equals(Object obj) {
        if(obj == this) return true;
        if(obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ListFieldNode) obj;
        return Objects.equals(this.value, that.value) &&
                Objects.equals(this.children, that.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, children);
    }

    @Override
    public String toString() {
        return "ListFieldNode[" +
                "value=" + value + ", " +
                "children=" + children + ']';
    }


}
