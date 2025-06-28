package de.safti.saftiSk.utils;

import com.google.common.primitives.Primitives;
import org.checkerframework.checker.signature.qual.ClassGetName;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class DarkMagic {
    private static final Logger log = LoggerFactory.getLogger(DarkMagic.class);

    public static <T> T readPrivateField(Object instance, String fieldName, Class<?> clazz) {
        return readPrivateField(instance, fieldName, null, clazz);
    }

    @SuppressWarnings("unchecked")
    public static <T> T readPrivateField(Object instance, String fieldName, Class<T> rValue, Class<?> clazz) {
        Field field;
        try {
            field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(instance);
        } catch (NoSuchFieldException e) {
            if(clazz == Object.class) {
                log.warn("Could not find field: {}", fieldName, e);
            }

            return readPrivateField(instance, fieldName, clazz.getSuperclass());

        } catch (IllegalAccessException e) {
            log.error("illegalAccessException:", e);
            throw new DarkMagicError(e);
        } catch (Throwable t) {
            log.error("Throwable: ", t);
            throw new DarkMagicError(t);
        }
    }

    @UnknownNullability
    public static <T> T invoke(Object instance, String methodName, Class<T> returnType, Object... args) throws ClassCastException {
        Class<?>[] argTypes = getTypes(args);

        try {
            Class<?> clazz = instance.getClass();
            Method method = clazz.getDeclaredMethod(methodName, argTypes);
            method.setAccessible(true);
            Object result = method.invoke(instance, args);

            //noinspection unchecked
            return (T) result;


        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            log.error("no such method", e);
            throw new DarkMagicError(e);
        }
    }

    @UnknownNullability
    public static <T> T invoke(Object instance, Method method, Class<T> returnType, Object... args) throws ClassCastException {

        try {
            method.setAccessible(true);
            Object result = method.invoke(instance, args);

            //noinspection unchecked
            return (T) result;


        } catch (InvocationTargetException | IllegalAccessException e) {
            log.error("no such method", e);
            throw new DarkMagicError(e);
        }
    }



    public static Method getMethod(Class<?> clazz, String name, Class<?>... argTypes) {
        try {
            return clazz.getDeclaredMethod(name, argTypes);
        } catch (NoSuchMethodException e) {
            throw new DarkMagicError(e);
        }
    }

    public static <T> T makeDefaultConstructorInstance(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            log.error("no default constructor found for class {}", clazz.getName());
            throw new DarkMagicError(e);
        }
    }

    public static <T> T construct(Class<T> clazz, Object... args) {
        try {
            var constructor = getConstructor(clazz, getTypes(args));
            constructor.setAccessible(true);
            return constructor.newInstance(args);

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new DarkMagicError(e);
        }
    }

    public static <T> Constructor<T> getConstructor(Class<T> clazz, Class<?>... paramTypes) {
        try {
            paramTypes = remapPrimitives(paramTypes);
            constructorLoop:
            for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                Class<?>[] constructorTypes = constructor.getParameterTypes();
                Class<?>[] actualParameters = new Class[constructorTypes.length];

                if(constructorTypes.length != paramTypes.length) {
                    continue;
                }

                for (int i = 0; i < constructorTypes.length; i++) {
                    Class<?> constructorParamType = constructorTypes[i];
                    // constructor param is super of given param
                    if(!constructorParamType.isAssignableFrom(paramTypes[i])) {
                        continue constructorLoop;
                    }
                    actualParameters[i] = constructorParamType;
                }
                // we found the constructor and we have the actual arguments
                return clazz.getDeclaredConstructor(actualParameters);

            }

            throw new DarkMagicError("no constructor matching paramTypes");
        } catch (NoSuchMethodException e) {
            log.error("this shouldn't happen.", e);
            throw new DarkMagicError(e);
        }


    }

    public static Class<?>[] remapPrimitives(Class<?>[] arr) {
        return Arrays.stream(arr)
               .map(DarkMagic::remapPrimitive)
               .toArray(Class[]::new);
    }

    public static <T> Class<T> remapPrimitive(Class<T> clazz) {
        return Primitives.unwrap(clazz); // default to google impl
    }

    public static Class<?>[] getTypes(Object... args) {
        Class<?>[] types = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i].getClass();
        }
        return remapPrimitives(types);
    }


    public static Class<?> getCallerClass(int skip) {
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(s -> s.skip(skip + 1).findFirst())
                .map(StackWalker.StackFrame::getDeclaringClass)
                .orElseThrow(() -> new IllegalStateException("Caller class not found"));
    }

    public static StackTraceElement getCallerElement(int skip) {
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(s -> s.skip(skip+1).findFirst())
                .map(StackWalker.StackFrame::toStackTraceElement)
                .orElseThrow(() -> new IllegalStateException("Caller class not found"));
    }

    public static Class<?> classByName(@ClassGetName String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new DarkMagicError(e);
        }
    }

    public static <R> R staticInvoke(Class<?> clazz, String methodName, Object... args) {
        var types = getTypes(args);
        try {
            Method method = clazz.getDeclaredMethod(methodName, types);
            method.setAccessible(true);
            //noinspection unchecked
            return (R) method.invoke(null, args);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new DarkMagicError(e);
        }

    }

    public static final class DarkMagicError extends RuntimeException {
        DarkMagicError(String message) {
            this(message, null);
        }

        DarkMagicError(String message, Throwable e) {
            super(message, e);
        }

        DarkMagicError(Throwable e) {
            super(e);
        }

    }

}
