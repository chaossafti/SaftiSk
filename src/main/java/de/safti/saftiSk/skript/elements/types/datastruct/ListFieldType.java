package de.safti.saftiSk.skript.elements.types.datastruct;

import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ListFieldType implements Map<String, Object> {
    private static final String SEPARATOR = "::";
    private final ListFieldNode root = new ListFieldNode(null, new HashMap<>());
    private int size;


    public static boolean isValidPath(String path) {
        String[] split = path.split("::");
        for (int i = 0, splitLength = split.length; i < splitLength; i++) {
            String s = split[i];

            if(s.equals("*") && i != splitLength-1) return false;
            if(s.isBlank()) return false;
        }

        return true;

    }


    @NotNull
    public ListFieldNode pathTo(String path) {
        validatePath(path);

        String[] directions = path.split(SEPARATOR);

        // traverse the hierarchy based on the directions
        ListFieldNode currentNode = root;
        for (String direction : directions) {
            if(direction.isEmpty()) {
                continue;
            }

            currentNode = currentNode.getOrMakeChild(direction);
        }

        return currentNode;
    }

    @Nullable
    public ListFieldNode lookup(String path) {
        String[] directions = path.split(SEPARATOR);

        // traverse the hierarchy based on the directions
        ListFieldNode currentNode = root;
        for (String direction : directions) {
            if(direction.isEmpty()) {
                continue;
            }

            currentNode = currentNode.getOrMakeChild(direction);
            if(currentNode == null) {
                return null;
            }
        }

        return currentNode;
    }

    public ListFieldNode lookupParent(String path) {
        String[] directions = path.split(SEPARATOR);

        // traverse the hierarchy based on the directions
        ListFieldNode currentNode = root;
        ListFieldNode parentNode = root;
        for (String direction : directions) {
            if(direction.isEmpty()) {
                continue;
            }

            if(direction.equals("*")) {
                return parentNode;
            }

            parentNode = currentNode;
            currentNode = currentNode.getOrMakeChild(direction);
            if(currentNode == null) {
                return null;
            }
        }

        return parentNode;
    }

    public List<ListFieldNode> traverse(String path) {
        String[] directions = path.split(SEPARATOR);
        List<ListFieldNode> nodesTraversed = new LinkedList<>();

        // traverse the hierarchy based on the directions
        ListFieldNode currentNode = root;
        for (String direction : directions) {
            if(direction.isEmpty()) {
                continue;
            }

            if(direction.equals("*")) {
                return nodesTraversed;
            }

            currentNode = currentNode.getOrMakeChild(direction);
            if(currentNode == null) {
                return null;
            }
            nodesTraversed.add(currentNode);
        }

        return nodesTraversed;
    }

    public Set<ListFieldNode> collectAllChildren() {
        Set<ListFieldNode> completed = new HashSet<>();

        Queue<ListFieldNode> nodeQueue = new ArrayDeque<>(collectChildren(completed, root));
        while(!nodeQueue.isEmpty()) {
            ListFieldNode node = nodeQueue.poll();
            nodeQueue.addAll(collectChildren(completed, node));
        }

        return completed;
    }

    public Set<ListFieldNode> collectChildren(Set<ListFieldNode> completed, ListFieldNode node) {
        Set<ListFieldNode> collectedChildren = new HashSet<>();

        for (ListFieldNode child : node.children().values()) {
            if(completed.contains(child)) continue;

            completed.add(child);
            collectedChildren.add(child);
        }

        return collectedChildren;
    }

    public Set<Pair<String, ListFieldNode>> collectAllChildrenWithPaths() {
        Set<ListFieldNode> completed = new HashSet<>();
        Set<Pair<String, ListFieldNode>> result = new HashSet<>();

        Queue<ListFieldNode> nodeQueue = new ArrayDeque<>(collectChildren(completed, root));
        while(!nodeQueue.isEmpty()) {
            ListFieldNode node = nodeQueue.poll();
            nodeQueue.addAll(collectChildren(completed, node));
        }

        return result;
    }

    public Set<ListFieldNode> collectChildren(Set<ListFieldNode> completed, Set<Pair<String, ListFieldNode>> result, ListFieldNode node, String path) {
        Set<ListFieldNode> collectedChildren = new HashSet<>();

        for (var entry : node.children().entrySet()) {
            var child = entry.getValue();
            var key = entry.getKey();
            if(completed.contains(child)) continue;

            completed.add(child);
            collectedChildren.add(child);
            result.add(Pair.of(path + "::" + key, child));
        }

        return collectedChildren;
    }

    public Iterator<ListFieldNode> nestedIterator() {
        return collectAllChildren().iterator();
    }

    public Stream<ListFieldNode> stream() {
        return collectAllChildren().stream();
    }

    public void validatePath(String path) {
        if(!isValidPath(path)) throw new IllegalArgumentException("Got invalid path: " + path);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return root.children().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        if(!(key instanceof String string)) {
            return false;
        }

        return lookup(string) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        return stream().anyMatch(listFieldNode -> listFieldNode.value() == value);
    }

    @Override
    public Object get(Object key) {
        if(!(key instanceof String string)) {
            return null;
        }

        ListFieldNode node = lookup(string);
        if(node == null) return null;
        return node.value();
    }

    public Object[] getMultiples(String path) {
        if(!path.endsWith("*")) {
            throw new IllegalArgumentException("Tried getting multiple values from single value path: " + path);
        }

        ListFieldNode parent = lookupParent(path);
        return parent.children().values()
                .stream()
                .map(ListFieldNode::value)
                .toArray();
    }

    @Override
    public @Nullable Object put(String key, Object value) {
        ListFieldNode node = pathTo(key);
        Object old = node.value();
        node.setValue(value);
        if(old == null) size++;
        return old;
    }

    @Override
    public Object remove(Object key) {
        if(!(key instanceof String string)) {
            return null;
        }

        String[] split = string.split(SEPARATOR);
        // simply remove from root if the path only contains one direction
        if(split.length < 2) {
            ListFieldNode remove = root.children().remove(key);
            if(remove != null) size--;
            return remove;
        }

        ListFieldNode parent = lookupParent(string);
        ListFieldNode remove = parent.children().remove(split[split.length - 1]);
        if(remove != null) size--;
        return remove;
    }

    @Override
    public void putAll(@NotNull Map<? extends String, ?> m) {
        for (var entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        root.children().clear();
        size = 0;
    }

    @Override
    public @NotNull Set<String> keySet() {
        return collectAllChildrenWithPaths()
                .stream()
                .map(Pair::key)
                .collect(Collectors.toSet());
    }

    @Override
    public @NotNull Collection<Object> values() {
        return stream()
                .map(ListFieldNode::value)
                .collect(Collectors.toSet());
    }

    @Override
    public @NotNull Set<Entry<String, Object>> entrySet() {
        return collectAllChildrenWithPaths()
                .stream()
                .map(pair -> Map.entry(pair.key(), pair.value().value()))
                .collect(Collectors.toSet());
    }
}
