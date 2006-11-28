/*******************************************************************************************
 * Copyright (c) Jonas Bonér, Alexandre Vasseur. All rights reserved.                      *
 * http://backport175.codehaus.org                                                         *
 * --------------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of Apache License Version 2.0 *
 * a copy of which has been included with this distribution in the license.txt file.       *
 *******************************************************************************************/
package com.tc.backport175;

import com.tc.backport175.bytecode.AnnotationReader;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for reader retrieval of strongly typed JavaDoc annotations (as well as regular Java 5 {@link
 * java.lang.reader.RetentionPolicy.RUNTIME} annotations  when running Java 1.5.x).
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public final class Annotations {

    /**
     * Checks if an annotation is present at a specific class.
     *
     * @param annotationType the annotation type
     * @param target the annotated type
     * @return true if the annotation is present else false
     */
    public static boolean isAnnotationPresent(final Class annotationType, final Class target) {
        boolean isPresent = AnnotationReader.getReaderFor(target).isAnnotationPresent(getAnnnotationName(annotationType));
        if (!isPresent && isInherited(annotationType)) {
            if (target.getSuperclass() == null) {
                return isPresent;
            } else {
                return isAnnotationPresent(annotationType, target.getSuperclass());
            }
        }
        return isPresent;
    }

    /**
     * Return all the annotations for a specific class.
     *
     * @param target          the java.lang.Class object to find the annotations on.
     * @return an array with the annotations
     */
    public static Annotation[] getAnnotations(final Class target) {
        Annotation[] declaredAnnotations = AnnotationReader.getReaderFor(target).getAnnotations();
        if (target.getSuperclass() == null) {
            return declaredAnnotations;
        } else {
            List annotations = new ArrayList(declaredAnnotations.length);
            Annotation[] parents = getAnnotations(target.getSuperclass());
            for (int i = 0; i < parents.length; i++) {
                if (isInherited(parents[i].annotationType())) {
                    annotations.add(parents[i]);
                }
            }
            for (int i = 0; i < declaredAnnotations.length; i++) {
                annotations.add(declaredAnnotations[i]);
            }
            return (Annotation[])annotations.toArray(new Annotation[]{});
        }
    }

    /**
     * Return the annotation with a specific name for a specific class.
     *
     * @param annotationType the annotation class
     * @param target      the java.lang.Class object to find the annotation on.
     * @return the annotation or null
     */
    public static Annotation getAnnotation(final Class annotationType, final Class target) {
        final AnnotationReader reader = AnnotationReader.getReaderFor(target);
        Annotation annotation = reader.getAnnotation(getAnnnotationName(annotationType));
        if (annotation == null && isInherited(annotationType)) {
            if (target.getSuperclass() == null) {
                return annotation;
            } else {
                return getAnnotation(annotationType, target.getSuperclass());
            }
        }
        return annotation;
    }

    /**
     * Checks if an annotation is present at a specific method.
     *
     * @param annotationType the annotation type
     * @param method the annotated type
     * @return true if the annotation is present else false
     */
    public static boolean isAnnotationPresent(final Class annotationType, final Method method) {
        final AnnotationReader reader = AnnotationReader.getReaderFor(method.getDeclaringClass());
        return reader.isAnnotationPresent(getAnnnotationName(annotationType), method);
    }

    /**
     * Return all the annotations for a specific method.
     *
     * @param method the java.lang.reflect.Method object to find the annotations on.
     * @return an array with the annotations
     */
    public static Annotation[] getAnnotations(final Method method) {
        return AnnotationReader.getReaderFor(method.getDeclaringClass()).getAnnotations(method);
    }

    /**
     * Return the annotation with a specific name for a specific method.
     *
     * @param annotationType the annotation class
     * @param method     the java.lang.refect.Method object to find the annotation on.
     * @return the annotation or null
     */
    public static Annotation getAnnotation(final Class annotationType, final Method method) {
        final AnnotationReader reader = AnnotationReader.getReaderFor(method.getDeclaringClass());
        return reader.getAnnotation(getAnnnotationName(annotationType), method);
    }

    /**
     * Checks if an annotation is present at a specific method.
     *
     * @param annotationType the annotation type
     * @param constructor the annotated type
     * @return true if the annotation is present else false
     */
    public static boolean isAnnotationPresent(final Class annotationType, final Constructor constructor) {
        final AnnotationReader reader = AnnotationReader.getReaderFor(constructor.getDeclaringClass());
        return reader.isAnnotationPresent(getAnnnotationName(annotationType), constructor);
    }

    /**
     * Return all the annotations for a specific constructor.
     *
     * @param constructor the java.lang.reflect.Constructor object to find the annotations on.
     * @return an array with the annotations
     */
    public static Annotation[] getAnnotations(final Constructor constructor) {
        return AnnotationReader.getReaderFor(constructor.getDeclaringClass()).getAnnotations(constructor);
    }

    /**
     * Return the annotation with a specific name for a specific constructor.
     *
     * @param annotationType  the annotation class
     * @param constructor the java.lang.refect.Constructor object to find the annotation on.
     * @return the annotation or null
     */
    public static Annotation getAnnotation(final Class annotationType, final Constructor constructor) {
        final AnnotationReader reader = AnnotationReader.getReaderFor(constructor.getDeclaringClass());
        return reader.getAnnotation(getAnnnotationName(annotationType), constructor);
    }

    /**
     * Checks if an annotation is present at a specific field.
     *
     * @param annotationType the annotation type
     * @param field the annotated type
     * @return true if the annotation is present else false
     */
    public static boolean isAnnotationPresent(final Class annotationType, final Field field) {
        final AnnotationReader reader = AnnotationReader.getReaderFor(field.getDeclaringClass());
        return reader.isAnnotationPresent(getAnnnotationName(annotationType), field);
    }

    /**
      * Return all the annotations for a specific field.
      *
      * @param field the java.lang.reflect.Field object to find the annotations on.
      * @return an array with the annotations
      */
     public static Annotation[] getAnnotations(final Field field) {
         return AnnotationReader.getReaderFor(field.getDeclaringClass()).getAnnotations(field);
     }

    /**
     * Return the annotation with a specific name for a specific field.
     *
     * @param annotationType the annotation class
     * @param field      the java.lang.reflect.Field object to find the annotation on.
     * @return the annotation or null
     */
    public static Annotation getAnnotation(final Class annotationType, final Field field) {
        final AnnotationReader reader = AnnotationReader.getReaderFor(field.getDeclaringClass());
        return reader.getAnnotation(getAnnnotationName(annotationType), field);
    }

    /**
     * Returns the annotation class name in Java style.
     *
     * @param annotationType
     * @return
     */
    private static String getAnnnotationName(final Class annotationType) {
        return annotationType.getName().replace('/', '.');
    }

    /**
     * Returns true if the annotation is @Inherited
     * @param annotationType
     * @return
     */
    private static boolean isInherited(final Class annotationType) {
        return AnnotationReader.getReaderFor(annotationType).isAnnotationPresent("java.lang.annotation.Inherited");
    }
}