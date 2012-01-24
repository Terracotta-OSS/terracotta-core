/*******************************************************************************************
 * Copyright (c) Jonas Boner, Alexandre Vasseur. All rights reserved.                      *
 * http://backport175.codehaus.org                                                         *
 * --------------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of Apache License Version 2.0 *
 * a copy of which has been included with this distribution in the license.txt file.       *
 *******************************************************************************************/
package com.tc.backport175.proxy;

import com.tc.backport175.bytecode.AnnotationElement;
import com.tc.backport175.bytecode.AnnotationReader;

import com.tc.asm.Type;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * Implements a strongly typed reader handler for JavaDoc annotations.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Boner</a>
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public class JavaDocAnnotationInvocationHander implements InvocationHandler, Serializable {
    static final long serialVersionUID = 1584167345753299421L;

    private static final String TO_STRING_METHOD_NAME = "toString";
    private static final String ANNOTATION_TYPE_METHOD_NAME = "annotationType";

    private final Class m_annotationInterface;
    private final AnnotationElement.Annotation m_annotation;
    private final String m_annotationName;

    /**
     * The annotated class classloader. Strong ref is ok since we use a proxy handler
     * and that one will be referenced by this classloader precisely
     */
    private final ClassLoader m_annotatedClassClassLoader;

    private ClassLoader getAnnotatedClassClassLoader() {
        return m_annotatedClassClassLoader;
    }

    /**
     * Constructor that will trigger the parsing if required
     *
     * @param annotationInterface
     * @param annotation
     * @param annotatedClassClassLoader classloader of the annotated class from which we can safely load all values
     */
    public JavaDocAnnotationInvocationHander(
            final Class annotationInterface,
            final AnnotationElement.Annotation annotation,
            final ClassLoader annotatedClassClassLoader) {
        m_annotationInterface = annotationInterface;
        m_annotation = annotation;
        m_annotationName = annotationInterface.getName().replace('/', '.');
        m_annotatedClassClassLoader = annotatedClassClassLoader!=null?annotatedClassClassLoader:ClassLoader.getSystemClassLoader();
    }

    /**
     * The proxy invoke method, dispatches to the target method being invoked.
     *
     * @param proxy
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        final String methodName = method.getName();
        if (methodName.equals(ANNOTATION_TYPE_METHOD_NAME)) {
            return m_annotationInterface;
        } else if (methodName.equals(TO_STRING_METHOD_NAME)) {
            return invokeToString();
        } else {
            return invokeAnnotationValue(method);
        }
    }

    /**
     * Handle invocation of an annotation value method.
     *
     * @param method
     * @return
     */
    private Object invokeAnnotationValue(final Method method) {
        Object returnValue = null;
        for (Iterator it = m_annotation.getElements().iterator(); it.hasNext();) {
            AnnotationElement.NamedValue namedValue = (AnnotationElement.NamedValue)it.next();
            if (namedValue.getName().equals(method.getName())) {
                returnValue = resolveValue(namedValue, method.getReturnType());
                break;
            }
        }
        return returnValue;
    }

    /**
     * Handles invocation of the <code>toString</code> method.
     *
     * @return the string representation for the annotation
     */
    private Object invokeToString() {
        StringBuffer sb = new StringBuffer();
        sb.append('@');
        sb.append(m_annotationName);
        sb.append('(');
        sb.append(m_annotation.toString());
        sb.append(')');
        return sb.toString();
    }

    /**
     * Returns the resolved value for the annotation element.
     *
     * @param namedValue
     * @param valueType
     * @return
     */
    private Object resolveValue(final AnnotationElement.NamedValue namedValue, final Class valueType) {
        if (namedValue.isResolved()) {
            return namedValue.getResolvedValue();
        }
        AnnotationElement.Type type = namedValue.getType();
        final Object value;
        if (type.equals(AnnotationElement.Type.ANNOTATION)) {
            AnnotationElement.Annotation annotation = (AnnotationElement.Annotation)namedValue.getValue();
            value = ProxyFactory.newAnnotationProxy(annotation, getAnnotatedClassClassLoader());

        } else if (type.equals(AnnotationElement.Type.ARRAY)) {
            value = resolveArray(namedValue, valueType);

        } else if (type.equals(AnnotationElement.Type.ENUM)) {
            value = resolveEnum(namedValue);

        } else if (type.equals(AnnotationElement.Type.TYPE)) {
            value = resolveType(namedValue);

        } else {
            value = namedValue.getValue();
        }
        namedValue.setResolvedValue(value);
        return value;
    }

    /**
     * Returns the class of an unresolved type.
     *
     * @param namedValue
     * @return
     */
    private Object resolveType(final AnnotationElement.NamedValue namedValue) {
        final Object value = namedValue.getValue();
        if (value instanceof Type) {
            // type
            final Type type = (Type)value;
            final Class resolvedType;
            try {
                if (type.getClassName().endsWith("[]")) {
                    //if (type.getDimensions() > 0) { // Note: Bugs in ASM prevents me from using this check, first: if type is primitive -> NPE, second: dimension is wrong for non-array types (1)
                    int dimensions = type.getDimensions();
                    Type elementType = type.getElementType();
                    Class componentType = resolveType(elementType);
                    resolvedType = Array.newInstance(componentType, new int[dimensions]).getClass();
                } else {
                    resolvedType = resolveType(type);
                }
            } catch (ClassNotFoundException cnfe) {
                throw new ResolveAnnotationException(
                        "class [" + type.getClassName() + "] defined in annotation can not be found in class loader [" +
                        m_annotatedClassClassLoader + "]", cnfe
                );
            }
            return resolvedType;
        } else {
            // primitive value
            return value;
        }
    }

    /**
     * Resolves a type.
     *
     * @param type
     * @return
     * @throws ClassNotFoundException
     */
    private Class resolveType(final Type type) throws ClassNotFoundException {
        Class resolvedType;
        if (Type.LONG_TYPE.equals(type)) {
            resolvedType = long.class;
        } else if (Type.INT_TYPE.equals(type)) {
            resolvedType = int.class;
        } else if (Type.SHORT_TYPE.equals(type)) {
            resolvedType = short.class;
        } else if (Type.DOUBLE_TYPE.equals(type)) {
            resolvedType = double.class;
        } else if (Type.FLOAT_TYPE.equals(type)) {
            resolvedType = float.class;
        } else if (Type.BOOLEAN_TYPE.equals(type)) {
            resolvedType = boolean.class;
        } else if (Type.BYTE_TYPE.equals(type)) {
            resolvedType = byte.class;
        } else if (Type.CHAR_TYPE.equals(type)) {
            resolvedType = char.class;
        } else {
            resolvedType = Class.forName(type.getClassName(), false, getAnnotatedClassClassLoader());
        }
        return resolvedType;
    }

    /**
     * Retuns the value of an enum (static field reference).
     *
     * @param namedValue
     * @return
     */
    private Object resolveEnum(final AnnotationElement.NamedValue namedValue) {
        AnnotationElement.Enum enumElement = (AnnotationElement.Enum)namedValue.getValue();
        String className = AnnotationReader.toJavaName(enumElement.getDesc());
        String value = enumElement.getValue();

        try {
            Class clazz = Class.forName(className, false, getAnnotatedClassClassLoader());
            Field field = clazz.getDeclaredField(value);
            try {
                return field.get(null);
            } catch (IllegalAccessException e) {
                throw new ResolveAnnotationException(
                        "can not access static reference field due to: " + e.toString(), e
                );
            }
        } catch (Exception e) {
            throw new ResolveAnnotationException(
                    "could not retrieve static reference to field (enum) [" + className + "." +
                    namedValue.getName() + "] due to: " + e.toString(), e
            );
        }
    }

    /**
     * Resolves the array type and returns an array instance of the correct type (including primitive types).
     *
     * @param namedValue
     * @param valueType
     * @return
     */
    private Object resolveArray(final AnnotationElement.NamedValue namedValue, final Class valueType) {
        if (!valueType.isArray()) {
            throw new IllegalArgumentException(
                    "annotation interface method [" + namedValue.getName() + "] in interface [" + m_annotationName +
                    "] needs to return an ARRAY type"
            );
        }
        AnnotationElement.Array array = (AnnotationElement.Array)namedValue.getValue();
        Class componentType = valueType.getComponentType();

        List arrayElements = array.getElements();
        List elementList = new ArrayList();
        for (Iterator it2 = arrayElements.iterator(); it2.hasNext();) {
            AnnotationElement.NamedValue arrayValue = (AnnotationElement.NamedValue)it2.next();

            // recursive call to resolveValue(..)
            elementList.add(resolveValue(arrayValue, componentType));
        }

        if (componentType.isPrimitive()) {
            if (componentType.equals(int.class)) {
                int[] arrayInstance = (int[])Array.newInstance(componentType, arrayElements.size());
                int i = 0;
                for (Iterator it = elementList.iterator(); it.hasNext();) {
                    Integer primitive = (Integer)it.next();
                    arrayInstance[i++] = primitive.intValue();
                }
                return arrayInstance;
            } else if (componentType.equals(long.class)) {
                long[] arrayInstance = (long[])Array.newInstance(componentType, arrayElements.size());
                int i = 0;
                for (Iterator it = elementList.iterator(); it.hasNext();) {
                    Long primitive = (Long)it.next();
                    arrayInstance[i++] = primitive.longValue();
                }
                return arrayInstance;
            } else if (componentType.equals(short.class)) {
                short[] arrayInstance = (short[])Array.newInstance(componentType, arrayElements.size());
                int i = 0;
                for (Iterator it = elementList.iterator(); it.hasNext();) {
                    Short primitive = (Short)it.next();
                    arrayInstance[i++] = primitive.shortValue();
                }
                return arrayInstance;
            } else if (componentType.equals(float.class)) {
                float[] arrayInstance = (float[])Array.newInstance(componentType, arrayElements.size());
                int i = 0;
                for (Iterator it = elementList.iterator(); it.hasNext();) {
                    Float primitive = (Float)it.next();
                    arrayInstance[i++] = primitive.floatValue();
                }
                return arrayInstance;
            } else if (componentType.equals(double.class)) {
                double[] arrayInstance = (double[])Array.newInstance(componentType, arrayElements.size());
                int i = 0;
                for (Iterator it = elementList.iterator(); it.hasNext();) {
                    Double primitive = (Double)it.next();
                    arrayInstance[i++] = primitive.doubleValue();
                }
                return arrayInstance;
            } else if (componentType.equals(char.class)) {
                char[] arrayInstance = (char[])Array.newInstance(componentType, arrayElements.size());
                int i = 0;
                for (Iterator it = elementList.iterator(); it.hasNext();) {
                    Character primitive = (Character)it.next();
                    arrayInstance[i++] = primitive.charValue();
                }
                return arrayInstance;
            } else if (componentType.equals(boolean.class)) {
                boolean[] arrayInstance = (boolean[])Array.newInstance(componentType, arrayElements.size());
                int i = 0;
                for (Iterator it = elementList.iterator(); it.hasNext();) {
                    Boolean primitive = (Boolean)it.next();
                    arrayInstance[i++] = primitive.booleanValue();
                }
                return arrayInstance;
            } else if (componentType.equals(byte.class)) {
                byte[] arrayInstance = (byte[])Array.newInstance(componentType, arrayElements.size());
                int i = 0;
                for (Iterator it = elementList.iterator(); it.hasNext();) {
                    Byte primitive = (Byte)it.next();
                    arrayInstance[i++] = primitive.byteValue();
                }
                return arrayInstance;
            }
        } else {
            Object[] arrayInstance = (Object[])Array.newInstance(componentType, arrayElements.size());
            int i = 0;
            for (Iterator it = elementList.iterator(); it.hasNext();) {
                Object element = it.next();
                arrayInstance[i++] = element;
            }
            return arrayInstance;
        }
        return null;
    }

}

