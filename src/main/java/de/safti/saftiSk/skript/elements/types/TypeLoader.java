package de.safti.saftiSk.skript.elements.types;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import ch.njol.yggdrasil.Fields;
import ch.njol.yggdrasil.YggdrasilSerializer;
import de.safti.saftiSk.skript.elements.types.datastruct.DataStructure;
import de.safti.saftiSk.skript.elements.types.datastruct.ListFieldNode;
import de.safti.saftiSk.skript.elements.types.datastruct.ListFieldType;
import de.safti.saftiSk.utils.DarkMagic;
import it.unimi.dsi.fastutil.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import java.util.*;

public class TypeLoader {

    private static final Logger log = LoggerFactory.getLogger(TypeLoader.class);

    public static void init() {


        Classes.registerClass(new ClassInfo<>(DataStructure.FieldContext.class, "datastructurefield")
                .user("data structure field")
                .serializer(new Serializer<>() {

                    @Override
                    public Fields serialize(DataStructure.FieldContext context) throws NotSerializableException {
                        if(!context.descriptor().clazz().isInstance(context.value())) {
                            throw new NotSerializableException("[DataStructures] Invalid value found in field!");
                        }


                        Fields fields = new Fields();
                        Class<?> clazz = context.descriptor().clazz();
                        ClassInfo<?> classInfo = Classes.getExactClassInfo(clazz);
                        if(classInfo == null || classInfo.getSerializer() == null) {
                            log.error("Could not serialize type {}", clazz.getName() + "! The field cannot be saved.");
                            throw new NotSerializableException("Field value could not be serialized.");
                        }

                        fields.putObject("t", classInfo.getCodeName());     // -> type
                        fields.putObject("v", context.value());             // -> value
                        fields.putObject("n", context.descriptor().name()); // -> name

                        return fields;
                    }

                    @Override
                    public void deserialize(DataStructure.FieldContext o, Fields f) throws StreamCorruptedException, NotSerializableException {
                        assert false;
                    }

                    @Override
                    protected DataStructure.FieldContext deserialize(Fields fields) throws StreamCorruptedException, NotSerializableException {
                        String classInfoCodeName = fields.getObject("t", String.class);
                        ClassInfo<?> classInfo = Classes.getClassInfo(classInfoCodeName);
                        Class<?> clazz = classInfo.getC();

                        Object value = fields.getObject("v", Object.class);
                        String name = fields.getObject("n", String.class);

                        DataStructure.FieldDescriptor descriptor = new DataStructure.FieldDescriptor(clazz, name);
                        return new DataStructure.FieldContext(descriptor, value);
                    }

                    @Override
                    public boolean mustSyncDeserialization() {
                        return false;
                    }

                    @Override
                    protected boolean canBeInstantiated() {
                        return false;
                    }
                })

        );

        Classes.registerClass(new ClassInfo<>(ListFieldType.class, "datastructurelistfield")
                .user("data structure list field", "list field", "list")
                .serializer(new Serializer<>() {

                    @Override
                    public Fields serialize(ListFieldType context) throws NotSerializableException {
                        Fields fields = new Fields();

                        // store all nodes
                        for (Pair<String, ListFieldNode> locationNodePair : context.collectAllChildrenWithPaths()) {
                            String location = locationNodePair.key();
                            ListFieldNode node = locationNodePair.value();
                            Object value = node.value();
                            if(value == null) continue;

                            // insert pair into Fields object
                            if(value.getClass().isPrimitive()) fields.putPrimitive(location, value);
                            else fields.putObject(location, value);
                        }

                        return fields;
                    }

                    @Override
                    protected ListFieldType deserialize(Fields fields) throws StreamCorruptedException, NotSerializableException {
                        Map<String, Fields.FieldContext> fieldContextMap = DarkMagic.readPrivateField(fields, "fields", Fields.class);
                        Map<String, Object> pathToValueMap = new HashMap<>();

                        // gather all values of fields inside fieldContextMap into pathToValueMap
                        for (String path : fieldContextMap.keySet()) {
                            Fields.FieldContext context = fieldContextMap.get(path);

                            // retrieve value from map
                            if(context.isPrimitive()) pathToValueMap.put(path, context.getPrimitive());
                            else pathToValueMap.put(path, fields.getObject(path));
                        }

                        // create ListFieldType from pathToValueMap
                        ListFieldType listFieldType = new ListFieldType();
                        listFieldType.putAll(pathToValueMap);
                        return listFieldType;
                    }

                    @Override
                    public boolean mustSyncDeserialization() {
                        return false;
                    }

                    @Override
                    protected boolean canBeInstantiated() {
                        return false;
                    }

                    @Override
                    public void deserialize(ListFieldType o, Fields f) throws StreamCorruptedException, NotSerializableException {
                        assert false;
                    }
                })

        );


        Classes.registerClass(new ClassInfo<>(DataStructure.class, "datastruct")
                .user("data struct", "data container", "value container")
                .serializer(new Serializer<>() {
                    @Override
                    public Fields serialize(DataStructure struct) throws NotSerializableException {
                        Fields fields = new Fields();
                        log.info("struct serialize; {}", struct);

                        // add values to Fields object
                        Collection<DataStructure.FieldContext> fieldContexts = struct.serialize();
                        List<String> serializedFields = new ArrayList<>();
                        for (DataStructure.FieldContext context : fieldContexts) {
                            if(context.value() == null) continue;

                            DataStructure.FieldDescriptor descriptor = context.descriptor();
                            String name = descriptor.name();

                            // TODO: make sure the value of the field can be serialized; if not log an error
                            fields.putObject("field_" + name, context);
                            serializedFields.add(name);
                        }

                        // add data structure data
                        fields.putObject("name", struct.getStructureName());
                        fields.putObject("fields", String.join(";", serializedFields));

                        log.info("fields when returning: {}", TypeLoader.toString(fields));
                        return fields;
                    }

                    @Override
                    public void deserialize(DataStructure o, Fields f) throws StreamCorruptedException, NotSerializableException {
                        assert false;

                    }

                    @Override
                    protected DataStructure deserialize(Fields fields) throws StreamCorruptedException, NotSerializableException {
                        log.info("struct deserialize; {}", TypeLoader.toString(fields));
                        String structureName = fields.getObject("name", String.class);
                        String joinedFieldNames = fields.getObject("fields", String.class);
                        String[] fieldNames = Objects.requireNonNullElse(joinedFieldNames, "").split(";");

                        Map<String, Object> valueMap = new HashMap<>();
                        // TODO: data fixers for renaming data structures
                        for (String fieldName : fieldNames) {
                            DataStructure.FieldContext context = fields.getObject("field_" + fieldName, DataStructure.FieldContext.class);
                            if(context == null) continue;

                            Object value = context.value();
                            valueMap.put(fieldName, value);
                        }

                        return new DataStructure(structureName, valueMap);
                    }

                    @Override
                    public boolean mustSyncDeserialization() {
                        return false;
                    }

                    @Override
                    protected boolean canBeInstantiated() {
                        return false;
                    }
                })

                .parser(new Parser<>() {

                    public boolean canParse(ParseContext context) {
                        return false;
                    }

                    @Override
                    public String toString(DataStructure structure, int flags) {
                        return structure.toString(); // send "%mytype%"
                    }

                    @Override
                    public String toVariableNameString(DataStructure struct) {
                        return struct.getStructureName(); // send "%mytype%" {players::%player%::%mytype%} to "hello type"
                    }
                }));
    }

    public static String toString(Fields fields) {
        Map<String, Object> map = DarkMagic.readPrivateField(fields, "fields", Fields.class);
        return map.keySet().toString();
    }


}
