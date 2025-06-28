package de.safti.saftiSk.skript.elements.types.datastruct;

import ch.njol.skript.variables.Variables;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DataStructure {
    private static final Map<String, Map<String, FieldDescriptor>> DATA_STRUCTURE_FIELD_MAP = new HashMap<>();

    private final String structureName;
    protected Map<String, Object> nameValueMap;

    static {
        Variables.yggdrasil.registerSingleClass(DataStructure.class, "SaftiSk_DataStructure");
    }

    public static Collection<FieldDescriptor> getFieldDescriptorsOf(String nameSpace) {
        if(!DATA_STRUCTURE_FIELD_MAP.containsKey(nameSpace)) {
            throw new IllegalArgumentException("no data structure with namepsace " + nameSpace + " found");
        }

        return DATA_STRUCTURE_FIELD_MAP.get(nameSpace).values();
    }

    public static void updateFields(String name, Map<String, FieldDescriptor> fields) {
        DATA_STRUCTURE_FIELD_MAP.put(name, fields);
    }

    public static DataStructure fromName(String lookingFor) {
        for (String name : DATA_STRUCTURE_FIELD_MAP.keySet()) {
            if(name.equals(lookingFor)) {
                return new DataStructure(name);
            }
        }

        return null;
    }

    public static boolean exists(String lookingFor) {
        for (String name : DATA_STRUCTURE_FIELD_MAP.keySet()) {
            if(name.equals(lookingFor)) {
                return true;
            }
        }
        return false;
    }

    public DataStructure(String structureName) {
        this.structureName = structureName;
        nameValueMap = new HashMap<>();
    }

    public DataStructure(String structureName, Map<String, Object> valueMap) {
        this.structureName = structureName;
        // intentionally ignored the fields existence; when deserialization of variables start not all DataStructures might be registered yet.
        this.nameValueMap = valueMap;
    }


    public void setValue(String fieldName, Object value) throws IllegalArgumentException {
        if(!hasField(fieldName)) {
            throw new IllegalArgumentException("No field named " + fieldName);
        }

        nameValueMap.put(fieldName, value);
    }

    public void delValue(String fieldName) throws IllegalArgumentException {
        if(!hasField(fieldName)) {
            throw new IllegalArgumentException("No field named " + fieldName);
        }

        nameValueMap.remove(fieldName);
    }

    public FieldDescriptor getDescriptor(String fieldName) {
        return DATA_STRUCTURE_FIELD_MAP
                .get(structureName)
                .get(fieldName);
    }


    public String getStructureName() {
        return structureName;
    }

    public boolean hasField(String fieldName) {
        return getDescriptor(fieldName) != null;
    }

    public Collection<FieldDescriptor> getFields() {
        return DATA_STRUCTURE_FIELD_MAP.get(structureName).values();
    }

    public Collection<String> getFieldNames() {
        return DATA_STRUCTURE_FIELD_MAP.get(structureName).keySet();
    }

    public Collection<FieldContext> serialize() {
        Set<FieldContext> result = new HashSet<>();
        for (String fieldName : getFieldNames()) {
            FieldDescriptor descriptor = getDescriptor(fieldName);
            Object o = nameValueMap.get(fieldName);

            result.add(new FieldContext(descriptor, o));
        }

        return result;
    }

    @Override
    public String toString() {
        return "DataStructure{" +
                "structureName='" + structureName + '\'' +
                ", nameValueMap=" + nameValueMap +
                '}';
    }

    @Nullable
    public Object get(String fieldName) throws IllegalArgumentException {
        if(!hasField(fieldName)) {
            throw new IllegalArgumentException("Field " + fieldName + " does not exist in data structure " + nameValueMap);
        }

        return nameValueMap.get(fieldName);
    }


    public record FieldDescriptor(Class<?> clazz, String name) {

        @Nullable
        public Object get(DataStructure structure) {
            return structure.get(name);
        }

        public boolean plural() {
            return clazz.isAssignableFrom(ListFieldType.class);
        }
    }

    public record FieldContext(FieldDescriptor descriptor, Object value) {

        public static FieldContext create(FieldDescriptor descriptor, Object value) throws IllegalArgumentException {
            // the value is not an instance of descriptor.clazz
            if(!descriptor.clazz.isInstance(value)) {
                throw new IllegalArgumentException("Provided value is not instance of descriptor class.");
            }

            return new FieldContext(descriptor, value);
        }
    }


}
