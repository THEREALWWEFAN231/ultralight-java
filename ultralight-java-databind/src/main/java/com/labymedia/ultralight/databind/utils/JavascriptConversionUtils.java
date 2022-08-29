/*
 * Ultralight Java - Java wrapper for the Ultralight web engine
 * Copyright (C) 2020 - 2022 LabyMedia and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.labymedia.ultralight.databind.utils;

import com.labymedia.ultralight.databind.Databind;
import com.labymedia.ultralight.databind.DatabindJavascriptClass;
import com.labymedia.ultralight.javascript.JavascriptClass;
import com.labymedia.ultralight.javascript.JavascriptContext;
import com.labymedia.ultralight.javascript.JavascriptObject;
import com.labymedia.ultralight.javascript.JavascriptType;
import com.labymedia.ultralight.javascript.JavascriptValue;

import java.lang.reflect.Array;
import java.util.Date;

/**
 * Helper for converting between Java and Javascript objects and classes.
 */
public final class JavascriptConversionUtils {
    private final Databind databind;

    /**
     * Constructs a new {@link JavascriptConversionUtils} instance using the given {@link Databind} instance for
     * translating objects.
     *
     * @param databind The {@link Databind} instance to use
     */
    public JavascriptConversionUtils(Databind databind) {
        this.databind = databind;
    }

    /**
     * Converts a Java object to a Javascript object.
     *
     * @param context The Javascript context to use for the conversion
     * @param object  The Java object to convert
     * @return The converted object as a Javascript value
     */
    public JavascriptValue toJavascript(JavascriptContext context, Object object) {
        return toJavascript(context, object, object != null ? object.getClass() : null);
    }

    /**
     * Converts a Java object to a Javascript object.
     *
     * @param context   The Javascript context to use for the conversion
     * @param object    The Java object to convert
     * @param javaClass The target java class to convert to
     * @return The converted object as a Javascript value
     */
    public JavascriptValue toJavascript(JavascriptContext context, Object object, Class<?> javaClass) {
        if (object == null || object == JavascriptType.NULL) {
            // Raw null
            return context.makeNull();
        } else if (object == JavascriptType.UNDEFINED) {
            // Raw undefined
            return context.makeUndefined();
        }

        javaClass = toWrapperClass(javaClass);

        // Decide based on the object's class
        if (javaClass == Boolean.class) {
            // Boolean conversion
            return context.makeBoolean((Boolean) object);
        } else if (Number.class.isAssignableFrom(javaClass)) {
            // All java integral types (except boolean and char) can be passed as doubles
            return context.makeNumber(((Number) object).doubleValue());
        } else if (javaClass == String.class) {
            // Strings are considered primitives in Javascript
            return context.makeString((String) object);
        } else if (javaClass.isArray()) {
            // Arrays required a recursive conversion
            // Reflective access to handle primitive arrays too
            int length = Array.getLength(object);
            JavascriptValue[] values = new JavascriptValue[length];

            for (int i = 0; i < length; i++) {
                // Recursive call
                Object value = Array.get(object, i);
                values[i] = toJavascript(context, value, value.getClass());
            }

            return context.makeArray(values);
        } else if (object instanceof Date) {
            // Dates are considered primitives in Javascript,
            // convert based on the unix epoch
            return context.makeDate(
                    context.makeNumber(((Date) object).getTime())
            );
        } else if (object instanceof JavascriptValue) {
            // Convert the object one-to-one
            JavascriptValue value = (JavascriptValue) object;
            if (value.isObject()) {
                // Cast the object if possible, or convert it to a Javascript object
                return value instanceof JavascriptObject ? (JavascriptObject) value : value.toObject();
            }

            return value;
        } else if (javaClass == Class.class) {
            // The base class needs special treatment
            return context.makeObject(
                    databind.toJavascript(javaClass, true),
                    new DatabindJavascriptClass.Data(null, javaClass));
        } else if (javaClass == JavascriptClass.class) {
            // The object is a javascript class already
            return context.makeObject((JavascriptClass) object, new DatabindJavascriptClass.Data(null, javaClass));
        }

        // Translate the object class
        return context.makeObject(databind.toJavascript(javaClass), new DatabindJavascriptClass.Data(object, javaClass));
    }

    /**
     * Converts a Javascript value to a Java object.
     *
     * @param value The Javascript value to convert
     * @param type  The type to convert the value to
     * @return The converted value
     */
    public Object fromJavascript(JavascriptValue value, Class<?> type) {
        JavascriptType javascriptType = value.getType();

        if (type == JavascriptValue.class) {
            return value;
        } else if (type == JavascriptObject.class) {
            if (javascriptType != JavascriptType.OBJECT) {
                throw new IllegalArgumentException("Can not convert a non-object Javascript value to " + type.getName());
            }

            // Cast the object if possible or re-construct
            return value instanceof JavascriptObject ? (JavascriptObject) value : value.toObject();
        }

        if (javascriptType == JavascriptType.NULL || javascriptType == JavascriptType.UNDEFINED || type == void.class || type == Void.class || type == null) {
            // Special handling of Javascript null and undefined
            if (type != null && type.isPrimitive() && type != void.class) {
                // Primitives can not be null in Java
                throw new IllegalArgumentException(
                        "Can not convert " + (javascriptType == JavascriptType.NULL ? "null" : "undefined") + " to " + type.getName());
            }

            // Map Javascript null and undefined to Java null
            return null;
        }

        // Flatten the type to reduce checks
        type = toPrimitiveClass(type);

        if (javascriptType == JavascriptType.BOOLEAN) {
            // Simple boolean conversion
            if (type != boolean.class && type != Object.class) {
                throw new IllegalArgumentException("Can not convert Javascript boolean to " + type.getName());
            }

            // One-to-one mapping
            return value.toBoolean();
        } else if (javascriptType == JavascriptType.NUMBER) {
            // Number conversion
            Number number = value.toNumber();
            if (type == Object.class) {
                return number;
            }

            if (type == byte.class) {
                return number.byteValue();
            } else if (type == short.class) {
                return number.shortValue();
            } else if (type == int.class) {
                return number.intValue();
            } else if (type == long.class) {
                return number.longValue();
            } else if (type == float.class) {
                return number.floatValue();
            } else if (type == double.class) {
                return number.doubleValue();
            } else if (type == char.class) {
                return (char) number.shortValue();
            }

            throw new IllegalArgumentException("Can not convert Javascript number to " + type.getName());
        } else if (javascriptType == JavascriptType.STRING) {
            String str = value.toString();

            if (type.isAssignableFrom(String.class) || type == Object.class) {
                // String can be passed on
                return str;
            } else if (type == char[].class) {
                // Use the char[] directly
                return str.toCharArray();
            } else if (type == Character[].class) {
                // Convert char[] to Character[]
                char[] primitiveArray = str.toCharArray();
                Character[] objectArray = new Character[primitiveArray.length];

                for (int i = 0; i < primitiveArray.length; i++) {
                    objectArray[i] = primitiveArray[i];
                }

                return objectArray;
            }

            throw new IllegalArgumentException("Can not convert Javascript string to " + type.getName());
        } else if (javascriptType == JavascriptType.OBJECT) {
            JavascriptObject object = value.toObject();
            if (value.isDate()) {
                // Date's are primitives in Javascript
                if (!type.isAssignableFrom(Date.class)) {
                    throw new IllegalArgumentException("Can not convert Javascript date to " + type.getName());
                }

                // Convert based on the unix epoch with the help of the getTime method of Javascript
                long millis = (long) object.getProperty("getTime")
                        .toObject().callAsFunction(value.toObject()).toNumber();
                return new Date(millis);
            } else if (value.isArray()) {
                // The target might use any object, so just convert the JS array to an Object[]
                boolean anyType = type == Object.class;

                if (!type.isArray() && !anyType) {
                    throw new IllegalArgumentException("Can not convert a Javascript array to " + type.getName());
                }

                // Prepare an array reflectively
                int size = (int) object.getProperty("length").toNumber();
                Class<?> componentType = anyType ? Object.class : type.getComponentType();
                Object objects = Array.newInstance(componentType, size);

                for (int i = 0; i < size; i++) {
                    // Recursively convert values
                    Array.set(objects, i, fromJavascript(object.getPropertyAtIndex(i), componentType));
                }

                return objects;
            } else if (databind.supportsFunctionalConversion() &&
                    object.isFunction() &&
                    type.isInterface() &&
                    type.isAnnotationPresent(FunctionalInterface.class)) {
                return FunctionalInterfaceBinder.bind(databind, type, object);
            }

            DatabindJavascriptClass.Data privateData = (DatabindJavascriptClass.Data) object.getPrivate();
            if (privateData == null) {
                // The Javascript object has not been constructed by java
                if (type == Object.class) {
                    return value;
                }

                throw new IllegalArgumentException(
                        "Can not convert a non Java constructed Javascript object to " + type.getName());
            }

            if (privateData.instance() == null) {
                if (!type.isAssignableFrom(Class.class) && type != Object.class) {
                    throw new IllegalArgumentException("Can not convert a Java class to " + type.getName());
                }

                return privateData.javaClass();
            } else {
                Object javaInstance = privateData.instance();
                if (!type.isAssignableFrom(javaInstance.getClass())) {
                    throw new IllegalArgumentException(
                            "Can not convert a " + javaInstance.getClass().getName() + " to " + type.getName());
                }

                return javaInstance;
            }
        }

        if (type == Object.class) {
            return value;
        }

        throw new IllegalArgumentException("Can not convert Javascript value to " + type.getName());
    }

    /**
     * Tries to infer the type from a Javascript value.
     *
     * @param value The value to infer the type from
     * @return The inferred type or null if the value is null or undefined
     */
    public static Class<?> determineType(JavascriptValue value) {
        JavascriptType type = value.getType();

        if (type == JavascriptType.NULL || type == JavascriptType.UNDEFINED) {
            return null;
        }

        // Check primitives first
        if (type == JavascriptType.BOOLEAN) {
            return boolean.class;
        } else if (type == JavascriptType.NUMBER) {
            return Number.class;
        } else if (type == JavascriptType.STRING) {
            return String.class;
        } else if (type == JavascriptType.OBJECT) {
            // Value is an object, deep check
            JavascriptObject object = value.toObject();

            if (value.isDate()) {
                // Date's are primitives in Javascript
                return Date.class;
            } else if (value.isArray()) {
                int size = (int) object.getProperty("length").toNumber();

                JavascriptValue[] values = new JavascriptValue[size];

                for (int i = 0; i < size; i++) {
                    values[i] = object.getPropertyAtIndex(i);
                }

                // Create an array with the common superclass
                return Array.newInstance(findCommonSuperclass(values), 0).getClass();
            }

            if (object.getPrivate() == null) {
                // Not a Java object
                return JavascriptObject.class;
            }

            DatabindJavascriptClass.Data privateData = (DatabindJavascriptClass.Data) object.getPrivate();

            if (privateData.instance() == null) {
                // Java class
                return privateData.javaClass();
            } else {
                // Instance of a Java object
                return privateData.instance().getClass();
            }
        }

        throw new AssertionError("UNREACHABLE: Could not convert JavascriptValue to any java class");
    }

    /**
     * Finds the most common superclass of all values (interfaces are not taken into account).
     *
     * @param values The values to find the most common superclass of
     * @return The most common superclass
     */
    private static Class<?> findCommonSuperclass(JavascriptValue... values) {
        Class<?>[] classes = new Class[values.length];

        // Convert all Javascript values to Java classes
        for (int i = 0; i < classes.length; i++) {
            Class<?> type = determineType(values[i]);
            classes[i] = type == null ? Object.class : type;
        }

        Class<?> commonSuperclass = classes[0];

        // Find the most common superclass
        outer:
        for (; commonSuperclass != Object.class; commonSuperclass = commonSuperclass.getSuperclass()) {
            for (int i = 1; i < classes.length; i++) {
                if (!commonSuperclass.isAssignableFrom(classes[i])) {
                    continue outer;
                }
            }
            return commonSuperclass;
        }

        return commonSuperclass;
    }

    /**
     * Converts a Java class to its primitive variant if possible.
     *
     * @param source The class to convert
     * @return The primitive version of the class, or source, if no primitive version is available
     */
    private static Class<?> toPrimitiveClass(Class<?> source) {
        if (source.isPrimitive()) {
            return source;
        }

        if (source == Boolean.class) {
            return boolean.class;
        } else if (source == Byte.class) {
            return byte.class;
        } else if (source == Character.class) {
            return char.class;
        } else if (source == Short.class) {
            return short.class;
        } else if (source == Integer.class) {
            return int.class;
        } else if (source == Long.class) {
            return long.class;
        } else if (source == Float.class) {
            return float.class;
        } else if (source == Double.class) {
            return double.class;
        }

        return source;
    }

    /**
     * Converts a Java class to its wrapper variant if required.
     *
     * @param source The class to convert
     * @return The wrapper version of the class, or source, if no wrapper version exists
     */
    private static Class<?> toWrapperClass(Class<?> source) {
        if (!source.isPrimitive()) {
            return source;
        }

        if (source == boolean.class) {
            return Boolean.class;
        } else if (source == byte.class) {
            return Byte.class;
        } else if (source == char.class) {
            return Character.class;
        } else if (source == short.class) {
            return Short.class;
        } else if (source == int.class) {
            return Integer.class;
        } else if (source == long.class) {
            return Long.class;
        } else if (source == float.class) {
            return Float.class;
        } else if (source == double.class) {
            return Double.class;
        }

        throw new AssertionError("UNREACHABLE: Primitive class passed, but no wrapper class known");
    }
}