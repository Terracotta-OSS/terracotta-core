/*******************************************************************************************
 * Copyright (c) Jonas Bonér, Alexandre Vasseur. All rights reserved.                      *
 * http://backport175.codehaus.org                                                         *
 * --------------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of Apache License Version 2.0 *
 * a copy of which has been included with this distribution in the license.txt file.       *
 *******************************************************************************************/
package com.tc.backport175.bytecode;

import com.tc.asm.AnnotationVisitor;
import com.tc.asm.ClassReader;
import com.tc.asm.FieldVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Type;
import com.tc.asm.commons.EmptyVisitor;
import com.tc.backport175.Annotation;
import com.tc.backport175.bytecode.spi.BytecodeProvider;
import com.tc.backport175.proxy.ProxyFactory;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Reads Java 5 {@link java.lang.annotation.RetentionPolicy.RUNTIME} and
 * {@link java.lang.annotation.RetentionPolicy.CLASS} annotations from the class' bytecode.
 * <p/>
 * Can be used with a custom implementation of the {@link org.codehaus.backport175.reader.bytecode.spi.BytecodeProvider}
 * interface.
 * <p/>
 * Note: does not handles {@link java.lang.annotation.Inherited} feature. This has to be done in the higher level
 * that knows about the class hierarchy (see backport175.Annotations f.e)
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér</a>
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public class AnnotationReader {

    private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];
    private static final AnnotationElement.Annotation[] EMPTY_ANNOTATION_ELEMENT_ARRAY =
            new AnnotationElement.Annotation[0];
    private static final String INIT_METHOD_NAME = "<init>";

    private static final Map CLASS_SPECIFIC_BYTECODE_PROVIDER = new WeakHashMap();
    private static volatile BytecodeProvider            BYTECODE_PROVIDER                = new DefaultBytecodeProvider();

  /**
   * Key is ClassKey, value is WeakReference of AnnotationReader
   */
    private static final Map READERS = new WeakHashMap();

    private final ClassKey m_classKey;

    // ===========================================================================
    // Implementation notes:
    // Parsing and annotation creation is made in two steps
    //
    // 1. The bytecode is parsed and the annotation content is put in elements,
    //    which are stored for later processing
    //
    // 2. Upon annotation access the elements are processed and a dynamic proxy
    //    for the annotation is created and cached.
    //
    // This gives much better performance than reflective access of Java 5
    // annotations (reflective access is around 5 times slower)
    // ===========================================================================

    private final Map m_classAnnotationElements = new HashMap();
    private final Map m_constructorAnnotationElements = new HashMap();
    private final Map m_methodAnnotationElements = new HashMap();
    private final Map m_fieldAnnotationElements = new HashMap();

    private final Map m_classAnnotationCache = new HashMap();
    private final Map m_constructorAnnotationCache = new HashMap();
    private final Map m_methodAnnotationCache = new HashMap();
    private final Map m_fieldAnnotationCache = new HashMap();

    /**
     * Sets the bytecode provider.
     * <p/>
     * If a custom provider is not set then a default impl will be used (which reads the bytecode from disk).
     *
     * @param bytecodeProvider
     */
    public static void setDefaultBytecodeProvider(final BytecodeProvider bytecodeProvider) {
        BYTECODE_PROVIDER = bytecodeProvider;
    }

    /**
     * Returns the bytecode provider.
     *
     * @return the bytecode provider
     */
    public static BytecodeProvider getDefaultBytecodeProvider() {
        return BYTECODE_PROVIDER;
    }

    /**
     * Sets the bytecode provider.
     * <p/>
     * If a custom provider is not set then a default impl will be used (which reads the bytecode from disk).
     *
     * @param klass
     * @param bytecodeProvider
     */
    public static void setBytecodeProviderFor(final Class klass, final BytecodeProvider bytecodeProvider) {
        setBytecodeProviderFor(klass.getName(), klass.getClassLoader(), bytecodeProvider);
    }

    /**
     * Sets the bytecode provider.
     * <p/>
     * If a custom provider is not set then a default impl will be used (which reads the bytecode from disk).
     *
     * @param className
     * @param loader
     * @param bytecodeProvider
     */
    public static void setBytecodeProviderFor(final String className,
                                              final ClassLoader loader,
                                              final BytecodeProvider bytecodeProvider) {
        CLASS_SPECIFIC_BYTECODE_PROVIDER.put(new ClassKey(className, loader), bytecodeProvider);
    }

    /**
     * Returns the bytecode provider.
     *
     * @param klass
     * @return the bytecode provider
     */
    public static BytecodeProvider getBytecodeProviderFor(final Class klass) {
        return getBytecodeProviderFor(klass.getName(), klass.getClassLoader());
    }

    /**
     * Returns the bytecode provider.
     *
     * @param className
     * @param loader
     * @return the bytecode provider
     */
    public static BytecodeProvider getBytecodeProviderFor(final String className, final ClassLoader loader) {
        BytecodeProvider bytecodeProvider = (BytecodeProvider) CLASS_SPECIFIC_BYTECODE_PROVIDER.get(
                new ClassKey(className, loader)
        );
        if (bytecodeProvider == null) {
            return BYTECODE_PROVIDER;
        }
        return bytecodeProvider;
    }

    /**
     * Returns the bytecode for a class.
     *
     * @param className
     * @param loader
     * @return the bytecode for a class
     */
    public static byte[] getBytecodeFor(final String className, final ClassLoader loader) throws ClassNotFoundException, IOException {
        return getBytecodeProviderFor(className, loader).getBytecode(className, loader);
    }

    /**
     * Returns the annotation reader for the class specified.
     * <p/>
     * The annotation reader is created and cached if non-existant.
     *
     * @param klass
     * @return the annotation reader
     */
    public static AnnotationReader getReaderFor(final Class klass) {
        return getReaderFor(new ClassKey(klass.getName(), klass.getClassLoader()));
    }

    /**
     * Returns the annotation reader for the class specified.
     * <p/>
     * The annotation reader is created and cached if non-existant.
     *
     * @param className
     * @param loader
     * @return the annotation reader
     */
    public static AnnotationReader getReaderFor(final String className, final ClassLoader loader) {
        return getReaderFor(new ClassKey(className, loader));
    }

    /**
     * Returns the annotation reader for the class specified.
     * <p/>
     * The annotation reader is created and cached if non-existant.
     *
     * @param classKey
     * @return the annotation reader
     */
    public static AnnotationReader getReaderFor(final ClassKey classKey) {
        final Reference ref;
        synchronized(READERS) {
           ref = (Reference) READERS.get(classKey);
        }

        AnnotationReader reader = (AnnotationReader) (ref == null ? null : ref.get());

        if (reader == null) {
            reader = new AnnotationReader(classKey);
            synchronized(READERS) {
                READERS.put(classKey, new WeakReference(reader));//reader strong refs its own key in the weakhahsmap..
            }
        }
        return reader;
    }

    /**
     * Resets the annotation reader for the class specified and triggers a new parsing of the newly read bytecode.
     * <p/>
     * This method calls <code>parse</code> and is therefore all the is needed to invoke to get a fully updated reader.
     *
     * @param klass
     */
    public static void refresh(final Class klass) {
        AnnotationReader reader = getReaderFor(klass);
        synchronized (reader) {
            reader.refresh();
        }
    }

    /**
     * Resets the annotation reader for the class specified and triggers a new parsing of the newly read bytecode.
     * <p/>
     * This method calls <code>parse</code> and is therefore all the is needed to invoke to get a fully updated reader.
     *
     * @param className
     * @param loader
     */
    public static void refresh(final String className, final ClassLoader loader) {
        AnnotationReader reader = getReaderFor(className, loader);
        synchronized (reader) {
            reader.refresh();
        }
    }

    /**
     * Resets *all* the annotation reader and triggers a new parsing of the newly read bytecode.
     * <p/>
     * This method will force parsing of all classes bytecode which might be very time consuming, use with care.
     * <p/>
     * This method calls <code>parse</code> and is therefore all the is needed to invoke to get a fully updated reader.
     */
    public static void refreshAll() {
      Object[] readers;
      synchronized (READERS) {
        readers = READERS.values().toArray();
      }

      for (int i = 0; i < readers.length; i++) {
        AnnotationReader reader = (AnnotationReader) ((Reference) readers[i]).get();
        if (reader != null) {
          synchronized (reader) {
            reader.refresh();
          }
        }
      }
    }

    /**
     * Converts the annotion class description to a Java class name.
     * Caution: Does not handles array type or primitive.
     *
     * @param desc
     * @return
     */
    public static String toJavaName(final String desc) {
        return desc.substring(1, desc.length() - 1).replace('/', '.');
    }

    /**
     * Checks if an annotation is present at a specific class.
     *
     * @param annotationName the annotation name
     * @return true if the annotation is present else false
     */
    public boolean isAnnotationPresent(final String annotationName) {
        return m_classAnnotationElements.containsKey(annotationName);
    }

    /**
     * Returns the class annotation with the name specified.
     *
     * @param annotationName
     * @return the class annotation
     */
    public Annotation getAnnotation(final String annotationName) {
        Object cachedAnnotation = m_classAnnotationCache.get(annotationName);
        if (cachedAnnotation != null) {
            return (Annotation) cachedAnnotation;
        } else {
            final Annotation annotation;
            final AnnotationElement.Annotation annotationInfo =
                    (AnnotationElement.Annotation) m_classAnnotationElements.get(annotationName);
            if (annotationInfo != null) {
                annotation = ProxyFactory.newAnnotationProxy(annotationInfo, m_classKey.getClassLoader());
                m_classAnnotationCache.put(annotationName, annotation);
                return annotation;
            } else {
                return null;
            }
        }
    }

    /**
     * Returns all the class annotations.
     *
     * @return an array with the class annotations
     */
    public Annotation[] getAnnotations() {
        final Collection annotationNames = m_classAnnotationElements.keySet();
        if (annotationNames.isEmpty()) {
            return EMPTY_ANNOTATION_ARRAY;
        }
        final Annotation[] annotations = new Annotation[annotationNames.size()];
        int i = 0;
        for (Iterator iterator = annotationNames.iterator(); iterator.hasNext();) {
            String annotationName = (String) iterator.next();
            annotations[i++] = getAnnotation(annotationName);
        }
        return annotations;
    }

    /**
     * Checks if an annotation is present at a specific constructor.
     *
     * @param annotationName the annotation name
     * @param constructor    the java.lang.reflect.Constructor object to find the annotations on.
     * @return true if the annotation is present else false
     */
    public boolean isAnnotationPresent(final String annotationName, final Constructor constructor) {
        final AnnotationReader.MemberKey key = AnnotationReader.MemberKey.newConstructorKey(constructor);
        Object map = m_constructorAnnotationElements.get(key);
        if (map != null) {
            if (((Map) map).containsKey(annotationName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the constructor annotation with the name specified for the constructor specified.
     *
     * @param annotationName the annotation name
     * @param constructor    the java.lang.reflect.Constructor object to find the annotations on.
     * @return the constructor annotation
     */
    public Annotation getAnnotation(final String annotationName, final Constructor constructor) {
        return getConstructorAnnotation(annotationName, MemberKey.newConstructorKey(constructor), constructor.getDeclaringClass().getClassLoader());
    }

    /**
     * Returns the constructor annotation with the name specified for the constructor specified.
     *
     * @param annotationName
     * @param constructorDesc
     * @param loader
     * @return
     */
    public Annotation getConstructorAnnotation(final String annotationName, final String constructorDesc, final ClassLoader loader) {
        return getConstructorAnnotation(annotationName, MemberKey.newConstructorKey(constructorDesc), loader);
    }

    /**
     * Returns the constructor annotation with the name specified for the constructor specified.
     *
     * @param annotationName
     * @param constructorKey
     * @param loader
     * @return
     */
    private Annotation getConstructorAnnotation(final String annotationName, final MemberKey constructorKey, final ClassLoader loader) {
        Map annotationMap = getConstructorAnnotationCacheFor(constructorKey);
        Object cachedAnnotation = annotationMap.get(annotationName);
        if (cachedAnnotation != null) {
            return (Annotation) cachedAnnotation;
        }
        // not in cache - create a new DP and put in cache
        final Map annotations = (Map) m_constructorAnnotationElements.get(constructorKey);
        if (annotations == null) {
            // no such annotation
            return null;
        }
        Object annotationElement = annotations.get(annotationName);
        if (annotationElement != null) {
            Annotation annotation = ProxyFactory.newAnnotationProxy(
                    (AnnotationElement.Annotation) annotationElement,
                    loader
            );
            annotationMap.put(annotationName, annotation);
            return annotation;
        }
        return null;
    }

    /**
     * Returns all the constructor annotations.
     *
     * @param constructor the java.lang.reflect.Constructor object to find the annotations on.
     * @return an array with the constructor annotations
     */
    public Annotation[] getAnnotations(final Constructor constructor) {
        final AnnotationReader.MemberKey key = AnnotationReader.MemberKey.newConstructorKey(constructor);
        Object map = m_constructorAnnotationElements.get(key);
        if (map != null) {
            final Collection annotationNames = ((Map) map).keySet();
            if (annotationNames.isEmpty()) {
                return EMPTY_ANNOTATION_ARRAY;
            }
            final Annotation[] annotations = new Annotation[annotationNames.size()];
            int i = 0;
            for (Iterator iterator = annotationNames.iterator(); iterator.hasNext();) {
                String annotationName = (String) iterator.next();
                annotations[i++] = getAnnotation(annotationName, constructor);
            }
            return annotations;
        } else {
            return EMPTY_ANNOTATION_ARRAY;
        }
    }

    /**
     * Checks if an annotation is present at a specific method.
     *
     * @param annotationName the annotation name
     * @param method         the java.lang.reflect.Method object to find the annotations on.
     * @return true if the annotation is present else false
     */
    public boolean isAnnotationPresent(final String annotationName, final Method method) {
        final AnnotationReader.MemberKey key = AnnotationReader.MemberKey.newMethodKey(method);
        Object map = m_methodAnnotationElements.get(key);
        if (map != null) {
            if (((Map) m_methodAnnotationElements.get(key)).containsKey(annotationName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the method annotation with the name specified for the method specified.
     *
     * @param annotationName the annotation name
     * @param method         the java.lang.reflect.Method object to find the annotations on.
     * @return the method annotation
     */
    public Annotation getAnnotation(final String annotationName, final Method method) {
        return getMethodAnnotation(
                annotationName,
                MemberKey.newMethodKey(method),
                method.getDeclaringClass().getClassLoader()
        );
    }

    /**
     * Returns the method annotation with the name specified for the method specified.
     *
     * @param annotationName
     * @param methodName
     * @param methodDesc
     * @param loader
     * @return
     */
    public Annotation getMethodAnnotation(final String annotationName, final String methodName, final String methodDesc, final ClassLoader loader) {
        return getMethodAnnotation(
                annotationName,
                MemberKey.newMethodKey(methodName, methodDesc),
                loader
        );
    }

    /**
     * Returns the method annotation with the name specified for the method specified.
     *
     * @param annotationName
     * @param methodKey
     * @param loader
     * @return
     */
    private Annotation getMethodAnnotation(final String annotationName, final MemberKey methodKey, final ClassLoader loader) {
        Map annotationMap = getMethodAnnotationCacheFor(methodKey);
        Object cachedAnnotation = annotationMap.get(annotationName);
        if (cachedAnnotation != null) {
            return (Annotation) cachedAnnotation;
        }
        // not in cache - create a new DP and put in cache
        final Map annotations = (Map) m_methodAnnotationElements.get(methodKey);
        if (annotations == null) {
            // no such annotation
            return null;
        }
        Object annotationElement = annotations.get(annotationName);
        if (annotationElement != null) {
            Annotation annotation = ProxyFactory.newAnnotationProxy(
                    (AnnotationElement.Annotation) annotationElement,
                    loader
            );
            annotationMap.put(annotationName, annotation);
            return annotation;
        }
        return null;
    }

    /**
     * Returns all the method annotations.
     *
     * @param method the java.lang.reflect.Method object to find the annotations on.
     * @return an array with the method annotations
     */
    public Annotation[] getAnnotations(final Method method) {
        final AnnotationReader.MemberKey key = AnnotationReader.MemberKey.newMethodKey(method);
        Object map = m_methodAnnotationElements.get(key);
        if (map != null) {
            final Collection annotationNames = ((Map) map).keySet();
            if (annotationNames.isEmpty()) {
                return EMPTY_ANNOTATION_ARRAY;
            }
            final Annotation[] annotations = new Annotation[annotationNames.size()];
            int i = 0;
            for (Iterator iterator = annotationNames.iterator(); iterator.hasNext();) {
                String annotationName = (String) iterator.next();
                annotations[i++] = getAnnotation(annotationName, method);
            }
            return annotations;
        } else {
            return EMPTY_ANNOTATION_ARRAY;
        }
    }

    /**
     * Checks if an annotation is present at a specific field.
     *
     * @param annotationName the annotation name
     * @param field          the java.lang.reflect.Field object to find the annotations on.
     * @return true if the annotation is present else false
     */
    public boolean isAnnotationPresent(final String annotationName, final Field field) {
        final AnnotationReader.MemberKey key = AnnotationReader.MemberKey.newFieldKey(field);
        Object map = m_fieldAnnotationElements.get(key);
        if (map != null) {
            if (((Map) map).containsKey(annotationName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the field annotation with the name specified for the field specified.
     *
     * @param annotationName the annotation name
     * @param field          the java.lang.reflect.Field object to find the annotations on.
     * @return the field annotation
     */
    public Annotation getAnnotation(final String annotationName, final Field field) {
        return getFieldAnnotation(
                annotationName,
                MemberKey.newFieldKey(field),
                field.getDeclaringClass().getClassLoader()
        );
    }

    /**
     * Returns the field annotation with the name specified for the field specified.
     *
     * @param annotationName
     * @param fieldName
     * @param fieldDesc
     * @param loader
     * @return
     */
    public Annotation getFieldAnnotation(final String annotationName, final String fieldName, final String fieldDesc, final ClassLoader loader) {
        return getFieldAnnotation(
                annotationName,
                MemberKey.newFieldKey(fieldName, fieldDesc),
                loader
        );
    }

    /**
     * Returns the field annotation with the name specified for the field specified.
     *
     * @param annotationName
     * @param fieldKey
     * @param loader
     * @return
     */
    private Annotation getFieldAnnotation(final String annotationName, final MemberKey fieldKey, final ClassLoader loader) {
        Map annotationMap = getFieldAnnotationCacheFor(fieldKey);
        Object cachedAnnotation = annotationMap.get(annotationName);
        if (cachedAnnotation != null) {
            return (Annotation) cachedAnnotation;
        }
        // not in cache - create a new DP and put in cache
        final Map annotations = (Map) m_fieldAnnotationElements.get(fieldKey);
        if (annotations == null) {
            // no such annotation
            return null;
        }
        Object annotationElement = annotations.get(annotationName);
        if (annotationElement != null) {
            Annotation annotation = ProxyFactory.newAnnotationProxy(
                    (AnnotationElement.Annotation) annotationElement,
                    loader
            );
            annotationMap.put(annotationName, annotation);
            return annotation;
        }
        return null;
    }

    /**
     * Returns all the field annotations.
     *
     * @param field the java.lang.reflect.Field object to find the annotations on.
     * @return an array with the field annotations
     */
    public Annotation[] getAnnotations(final Field field) {
        final AnnotationReader.MemberKey key = AnnotationReader.MemberKey.newFieldKey(field);
        Object map = m_fieldAnnotationElements.get(key);
        if (map != null) {
            final Collection annotationNames = ((Map) map).keySet();
            if (annotationNames.isEmpty()) {
                return EMPTY_ANNOTATION_ARRAY;
            }
            final Annotation[] annotations = new Annotation[annotationNames.size()];
            int i = 0;
            for (Iterator iterator = annotationNames.iterator(); iterator.hasNext();) {
                String annotationName = (String) iterator.next();
                annotations[i++] = getAnnotation(annotationName, field);
            }
            return annotations;
        } else {
            return EMPTY_ANNOTATION_ARRAY;
        }
    }

    /**
     * Returns the class annotation element with the name specified.
     *
     * @param annotationName
     * @return the class annotation
     */
    public AnnotationElement.Annotation getAnnotationElement(final String annotationName) {
        return (AnnotationElement.Annotation) m_classAnnotationElements.get(annotationName);
    }

    /**
     * Returns all the class annotations.
     *
     * @return an array with the class annotations
     */
    public AnnotationElement.Annotation[] getAnnotationElements() {
        final Collection annotations = m_classAnnotationElements.values();
        if (annotations.isEmpty()) {
            return EMPTY_ANNOTATION_ELEMENT_ARRAY;
        }
        return createAnnotationElementArray(annotations);
    }

    /**
     * Checks if an annotation is present at a specific constructor.
     *
     * @param annotationName the annotation name
     * @param desc           the constructor desc
     * @return true if the annotation is present else false
     */
    public boolean isConstructorAnnotationPresent(final String annotationName, final String desc) {
        final AnnotationReader.MemberKey key = AnnotationReader.MemberKey.newConstructorKey(desc);
        Object map = m_constructorAnnotationElements.get(key);
        if (map != null) {
            if (((Map) map).containsKey(annotationName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the annotation with the name specified for the constructor specified.
     *
     * @param annotationName the annotation name
     * @param desc           the constructor desc
     * @return the constructor annotation element
     */
    public AnnotationElement.Annotation getConstructorAnnotationElement(final String annotationName, final String desc) {
        final AnnotationReader.MemberKey key = AnnotationReader.MemberKey.newConstructorKey(desc);
        final Map annotations = (Map) m_constructorAnnotationElements.get(key);
        if (annotations == null) {
            // no such annotation
            return null;
        }
        return (AnnotationElement.Annotation) annotations.get(annotationName);
    }

    /**
     * Returns all the constructor annotation elements.
     *
     * @param desc the constructor desc
     * @return an array with the constructor annotation elements
     */
    public AnnotationElement.Annotation[] getConstructorAnnotationElements(final String desc) {
        final AnnotationReader.MemberKey key = AnnotationReader.MemberKey.newConstructorKey(desc);
        Object map = m_constructorAnnotationElements.get(key);
        if (map != null) {
            final Collection annotations = ((Map) map).values();
            if (annotations.isEmpty()) {
                return EMPTY_ANNOTATION_ELEMENT_ARRAY;
            }
            return createAnnotationElementArray(annotations);
        } else {
            return EMPTY_ANNOTATION_ELEMENT_ARRAY;
        }
    }

    /**
     * Checks if an annotation is present at a specific method.
     *
     * @param annotationName the annotation name
     * @param name           the method name
     * @param desc           the method desc
     * @return true if the annotation is present else false
     */
    public boolean isMethodAnnotationPresent(final String annotationName, final String name, final String desc) {
        final AnnotationReader.MemberKey key = AnnotationReader.MemberKey.newMethodKey(name, desc);
        Object map = m_methodAnnotationElements.get(key);
        if (map != null) {
            if (((Map) map).containsKey(annotationName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the method annotation with the name specified for the method specified.
     *
     * @param annotationName the annotation name
     * @param name           the method name
     * @param desc           the method desc
     * @return the method annotation element
     */
    public AnnotationElement.Annotation getMethodAnnotationElement(final String annotationName,
                                                                   final String name,
                                                                   final String desc) {
        final AnnotationReader.MemberKey key = AnnotationReader.MemberKey.newMethodKey(name, desc);
        final Map annotations = (Map) m_methodAnnotationElements.get(key);
        if (annotations == null) {
            // no such annotation
            return null;
        }
        return (AnnotationElement.Annotation) annotations.get(annotationName);
    }

    /**
     * Returns all the method annotation elements.
     *
     * @param name the method name
     * @param desc the method desc
     * @return an array with the method annotation elements
     */
    public AnnotationElement.Annotation[] getMethodAnnotationElements(final String name, final String desc) {
        final AnnotationReader.MemberKey key = AnnotationReader.MemberKey.newMethodKey(name, desc);
        Object map = m_methodAnnotationElements.get(key);
        if (map != null) {
            final Collection annotations = ((Map) m_methodAnnotationElements.get(key)).values();
            if (annotations.isEmpty()) {
                return EMPTY_ANNOTATION_ELEMENT_ARRAY;
            }
            return createAnnotationElementArray(annotations);
        } else {
            return EMPTY_ANNOTATION_ELEMENT_ARRAY;
        }
    }

    /**
     * Checks if an annotation is present at a specific field.
     *
     * @param annotationName the annotation name
     * @param name           the field name
     * @param desc           the field desc
     * @return true if the annotation is present else false
     */
    public boolean isFieldAnnotationPresent(final String annotationName, final String name, final String desc) {
        final AnnotationReader.MemberKey key = AnnotationReader.MemberKey.newFieldKey(name, desc);
        Object map = m_fieldAnnotationElements.get(key);
        if (map != null) {
            if (((Map) map).containsKey(annotationName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the annotation with the name specified for the field specified.
     *
     * @param annotationName the annotation name
     * @param name           the field name
     * @param desc           the field desc
     * @return the field annotation element
     */
    public AnnotationElement.Annotation getFieldAnnotationElement(final String annotationName,
                                                                  final String name,
                                                                  final String desc) {
        final AnnotationReader.MemberKey key = AnnotationReader.MemberKey.newFieldKey(name, desc);
        final Map annotations = (Map) m_fieldAnnotationElements.get(key);
        if (annotations == null) {
            // no such annotation
            return null;
        }
        return (AnnotationElement.Annotation) annotations.get(annotationName);
    }

    /**
     * Returns all the field annotation elements.
     *
     * @param name the field name
     * @param desc the field desc
     * @return an array with the field annotation elements
     */
    public AnnotationElement.Annotation[] getFieldAnnotationElements(final String name, final String desc) {
        final AnnotationReader.MemberKey key = AnnotationReader.MemberKey.newFieldKey(name, desc);
        Object map = m_fieldAnnotationElements.get(key);
        if (map != null) {
            final Collection annotations = ((Map) m_fieldAnnotationElements.get(key)).values();
            if (annotations.isEmpty()) {
                return EMPTY_ANNOTATION_ELEMENT_ARRAY;
            }
            return createAnnotationElementArray(annotations);
        } else {
            return EMPTY_ANNOTATION_ELEMENT_ARRAY;
        }
    }

    /**
     * Creates an annotation element array.
     *
     * @param annotations the collection with elements
     * @return the array
     */
    private AnnotationElement.Annotation[] createAnnotationElementArray(final Collection annotations) {
        int i = 0;
        final AnnotationElement.Annotation[] elementArray = new AnnotationElement.Annotation[annotations.size()];
        for (Iterator it = annotations.iterator(); it.hasNext();) {
            elementArray[i++] = (AnnotationElement.Annotation) it.next();
        }
        return elementArray;
    }

    /**
     * Returns the annotation cache for a specific constructor.
     *
     * @param constructor the constructor
     * @return the cache
     */
    private Map getConstructorAnnotationCacheFor(final MemberKey constructor) {
        Map annotationMap = (Map) m_constructorAnnotationCache.get(constructor);
        if (annotationMap == null) {
            annotationMap = new HashMap();
            m_constructorAnnotationCache.put(constructor, annotationMap);
        }
        return annotationMap;
    }

    /**
     * Returns the annotation cache for a specific method.
     *
     * @param method the method
     * @return the cache
     */
    private Map getMethodAnnotationCacheFor(final MemberKey method) {
        Map annotationMap = (Map) m_methodAnnotationCache.get(method);
        if (annotationMap == null) {
            annotationMap = new HashMap();
            m_methodAnnotationCache.put(method, annotationMap);
        }
        return annotationMap;
    }

    /**
     * Returns the annotation cache for a specific field.
     *
     * @param field the field
     * @return the cache
     */
    private Map getFieldAnnotationCacheFor(final MemberKey field) {
        Map annotationMap = (Map) m_fieldAnnotationCache.get(field);
        if (annotationMap == null) {
            annotationMap = new HashMap();
            m_fieldAnnotationCache.put(field, annotationMap);
        }
        return annotationMap;
    }

    /**
     * Resets the annotation reader and triggers a new parsing of the newly read bytecode.
     * <p/>
     * This method calls <code>parse</code> and is therefore all the is needed to invoke to get a fully updated reader.
     */
    private void refresh() {
        m_classAnnotationElements.clear();
        m_constructorAnnotationElements.clear();
        m_methodAnnotationElements.clear();
        m_fieldAnnotationElements.clear();
        m_classAnnotationCache.clear();
        m_constructorAnnotationCache.clear();
        m_methodAnnotationCache.clear();
        m_fieldAnnotationCache.clear();
        AnnotationDefaults.refresh(m_classKey);
        parse(m_classKey);
    }

    /**
     * Parses the class bytecode and retrieves the annotations.
     *
     * @param classKey
     */
    private void parse(final ClassKey classKey) {
        final String className = classKey.getName();
        final ClassLoader loader = classKey.getClassLoader();
        final byte[] bytes;
        try {
            bytes = getBytecodeFor(className, loader);
        } catch (ClassNotFoundException e) {
          return;
        } catch (Exception e) {
          // e.printStackTrace();
          System.err.println("[WARN] " + e.getMessage());
          return;
//            throw new ReaderException(
//                    "could not retrieve the bytecode for class [" + className + "]", e
//            );
        }
        ClassReader classReader = new ClassReader(bytes);
    classReader.accept(new AnnotationRetrievingVisitor(), ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE
                                                          | ClassReader.SKIP_FRAMES);
    }

    /**
     * Creates a new instance of the annotation reader, reads from the class specified.
     *
     * @param classKey
     */
    private AnnotationReader(final ClassKey classKey) {
        if (classKey == null) {
            throw new IllegalArgumentException("class info can not be null");
        }
        m_classKey = classKey;
        parse(classKey);
    }

    /**
     * Retrieves the Java 5 RuntimeVisibleAnnotations annotations from the class bytecode.
     */
    class AnnotationRetrievingVisitor extends EmptyVisitor {

        public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
            String annotationClassName = toJavaName(desc);
            final AnnotationElement.Annotation annotation = new AnnotationElement.Annotation(annotationClassName);
            m_classAnnotationElements.put(annotationClassName, annotation);
            return createAnnotationVisitor(annotation);
        }

        public FieldVisitor visitField(final int access,
                                       final String name,
                                       final String desc,
                                       final String signature,
                                       final Object value) {
            final MemberKey key = new MemberKey(name, desc);
            return new AnnotationRetrievingFieldVisitor(key, AnnotationReader.this);
        }

        public MethodVisitor visitMethod(final int access,
                                         final String name,
                                         final String desc,
                                         final String signature,
                                         final String[] exceptions) {
            final MemberKey key = new MemberKey(name, desc);
            if (name.equals(INIT_METHOD_NAME)) {
                return new AnnotationRetrievingConstructorVisitor(key, AnnotationReader.this);
            } else {
                return new AnnotationRetrievingMethodVisitor(key, AnnotationReader.this);
            }
        }

    }

    /**
     * Returns the annotation visitor to use.
     * <p/>
     * Swap to the 'tracing' visitor for simple debugging.
     *
     * @param annotation
     * @return
     */
    public AnnotationVisitor createAnnotationVisitor(final AnnotationElement.Annotation annotation) {
      return new AnnotationBuilderVisitor(annotation, m_classKey.getClassLoader(), annotation.getInterfaceName());
//            return new TraceAnnotationVisitor();
    }

    static final class AnnotationRetrievingConstructorVisitor extends EmptyVisitor {
      private final MemberKey key;
      private final AnnotationReader reader;

      AnnotationRetrievingConstructorVisitor(MemberKey key, AnnotationReader reader) {
        this.key = key;
        this.reader = reader;
      }

      public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
          final String className = toJavaName(desc);
          final AnnotationElement.Annotation annotation = new AnnotationElement.Annotation(className);
          if (reader.m_constructorAnnotationElements.containsKey(key)) {
              ((Map) reader.m_constructorAnnotationElements.get(key)).put(className, annotation);
          } else {
              final Map annotations = new HashMap();
              annotations.put(className, annotation);
              reader.m_constructorAnnotationElements.put(key, annotations);
          }
          return reader.createAnnotationVisitor(annotation);
      }
    }

    static final class AnnotationRetrievingMethodVisitor extends EmptyVisitor {
      private final MemberKey key;
      private final AnnotationReader reader;

      AnnotationRetrievingMethodVisitor(MemberKey key, AnnotationReader reader) {
        this.key = key;
        this.reader = reader;
      }

      public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
          String className = toJavaName(desc);
          final AnnotationElement.Annotation annotation = new AnnotationElement.Annotation(className);
          if (reader.m_methodAnnotationElements.containsKey(key)) {
              ((Map) reader.m_methodAnnotationElements.get(key)).put(className, annotation);
          } else {
              final Map annotations = new HashMap();
              annotations.put(className, annotation);
              reader.m_methodAnnotationElements.put(key, annotations);
          }
          return reader.createAnnotationVisitor(annotation);
      }
    }

    static final class AnnotationRetrievingFieldVisitor extends EmptyVisitor {
      private final MemberKey    key;
      private final AnnotationReader reader;

      AnnotationRetrievingFieldVisitor(MemberKey key, AnnotationReader reader) {
        this.key = key;
        this.reader = reader;
      }

      public AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
          final String className = toJavaName(desc);
          final AnnotationElement.Annotation annotation = new AnnotationElement.Annotation(className);
          if (reader.m_fieldAnnotationElements.containsKey(key)) {
              ((Map) reader.m_fieldAnnotationElements.get(key)).put(className, annotation);
          } else {
              final Map annotations = new HashMap();
              annotations.put(className, annotation);
              reader.m_fieldAnnotationElements.put(key, annotations);
          }
          return reader.createAnnotationVisitor(annotation);
      }

    }


    static class AnnotationBuilderVisitor implements AnnotationVisitor {

        private final AnnotationElement.NestedAnnotationElement m_nestedAnnotationElement;

    /**
     * ClassLoader from which both the annotated element and its annoation(s) are visible
     */
        private final ClassLoader m_loader;

        /**
         * Annotation class name. If not null, default values will be handled, else it will be skip.
         * (f.e. skip for nested annotation and arrays)
         */
        private final String m_annotationClassName;

        public AnnotationBuilderVisitor(final AnnotationElement.NestedAnnotationElement annotation,
                                        final ClassLoader loader,
                                        final String annotationClassName) {
            m_nestedAnnotationElement = annotation;
            m_loader = loader;
            m_annotationClassName = annotationClassName;
        }

        public void visit(final String name, final Object value) {
            if (value instanceof Type) {
                // type
                m_nestedAnnotationElement.addElement(name, value);
            } else {
                // primitive value
                if (value.getClass().isArray()) {
                    // primitive array value
                    handlePrimitiveArrayValue(value, name);
                } else {
                    // primitive non-array value
                    m_nestedAnnotationElement.addElement(name, value);
                }
            }
        }

        public void visitEnum(final String name, final String desc, final String value) {
            m_nestedAnnotationElement.addElement(name, new AnnotationElement.Enum(desc, value));
        }

        public AnnotationVisitor visitAnnotation(final String name, final String desc) {
            String className = toJavaName(desc);
            AnnotationElement.NestedAnnotationElement annotation = new AnnotationElement.Annotation(className);
            m_nestedAnnotationElement.addElement(name, annotation);
            return new AnnotationBuilderVisitor(annotation, m_loader, className);//recursive default handling
        }

        public AnnotationVisitor visitArray(final String name) {
            AnnotationElement.NestedAnnotationElement array = new AnnotationElement.Array();
            m_nestedAnnotationElement.addElement(name, array);
            return new AnnotationBuilderVisitor(array, m_loader, null);
        }

        public void visitEnd() {
            // annotation default overrides
            if (m_annotationClassName != null) {
                AnnotationElement.Annotation defaults = AnnotationDefaults.getDefaults(m_annotationClassName, m_loader);
                if (defaults != null) {
                  AnnotationElement.Annotation annotation = (AnnotationElement.Annotation) m_nestedAnnotationElement;
                  for (Iterator iterator = defaults.getElements().iterator(); iterator.hasNext();) {
                      AnnotationElement.NamedValue defaultedElement = (AnnotationElement.NamedValue) iterator.next();
                      annotation.mergeDefaultedElement(defaultedElement);
                  }
                }
            }
        }

        /**
         * Handles array of primitive values. The JSR-175 spec. only suppots one dimensional arrays.
         *
         * @param value
         * @param name
         */
        private void handlePrimitiveArrayValue(final Object value, final String name) {
            if (value.getClass().getComponentType().isPrimitive()) {
                // primitive array type
                if (value instanceof String[]) {
                    // string array
                    m_nestedAnnotationElement.addElement(name, value);
                } else {
                    AnnotationElement.NestedAnnotationElement arrayElement = new AnnotationElement.Array();
                    // non-string primitive array
                    if (value instanceof int[]) {
                        int[] array = (int[]) value;
                        for (int i = 0; i < array.length; i++) {
                            arrayElement.addElement(null, new Integer(array[i]));
                        }
                    } else if (value instanceof long[]) {
                        long[] array = (long[]) value;
                        for (int i = 0; i < array.length; i++) {
                            arrayElement.addElement(null, new Long(array[i]));
                        }
                    } else if (value instanceof short[]) {
                        short[] array = (short[]) value;
                        for (int i = 0; i < array.length; i++) {
                            arrayElement.addElement(null, new Short(array[i]));
                        }
                    } else if (value instanceof float[]) {
                        float[] array = (float[]) value;
                        for (int i = 0; i < array.length; i++) {
                            arrayElement.addElement(null, new Float(array[i]));
                        }
                    } else if (value instanceof double[]) {
                        double[] array = (double[]) value;
                        for (int i = 0; i < array.length; i++) {
                            arrayElement.addElement(null, new Double(array[i]));
                        }
                    } else if (value instanceof boolean[]) {
                        boolean[] array = (boolean[]) value;
                        for (int i = 0; i < array.length; i++) {
                            arrayElement.addElement(null, new Boolean(array[i]));
                        }
                    } else if (value instanceof byte[]) {
                        byte[] array = (byte[]) value;
                        for (int i = 0; i < array.length; i++) {
                            arrayElement.addElement(null, new Byte(array[i]));
                        }
                    } else if (value instanceof char[]) {
                        char[] array = (char[]) value;
                        for (int i = 0; i < array.length; i++) {
                            arrayElement.addElement(null, new Character(array[i]));
                        }
                    }
                    m_nestedAnnotationElement.addElement(name, arrayElement);
                }
            } else {
                m_nestedAnnotationElement.addElement(name, value);
            }
        }
    }

    /**
     * Contains info about the class being parsed. Holds the class name and a weak ref to the class loader. Also works
     * as a unique key. Needed since at bytecode parsing time we do not have access to the reflect members, only
     * strings.
     *
     * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér</a>
     */
    public static class ClassKey {
        private final String m_name;
        private final WeakReference m_loaderRef;

        public ClassKey(final String name, final ClassLoader loader) {
            m_name = name.replace('.', '/');
            m_loaderRef = new WeakReference(loader);
        }

        public String getName() {
            return m_name;
        }

        public ClassLoader getClassLoader() {
            return (ClassLoader) m_loaderRef.get();
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ClassKey)) {
                return false;
            }
            final ClassKey classKey = (ClassKey) o;
            ClassLoader loader1 = (ClassLoader) m_loaderRef.get();
            ClassLoader loader2 = (ClassLoader) classKey.m_loaderRef.get();
            if (loader1 != null ? !loader1.equals(loader2) : loader2 != null) {
                return false;
            }
            if (m_name != null ? !m_name.equals(classKey.m_name) : classKey.m_name != null) {
                return false;
            }
            return true;
        }

        public int hashCode() {
            int result;
            result = (m_name != null ? m_name.hashCode() : 0);
            ClassLoader loader = (ClassLoader) m_loaderRef.get();
            result = 29 * result + (loader != null ? loader.hashCode() : 0);
            return result;
        }
    }

    /**
     * Unique key for class members (methods, fields and constructors) to be used in hash maps etc.
     * <p/>
     * Needed since at bytecode parsing time we do not have access to the reflect members, only strings.
     *
     * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér</a>
     */
    public static class MemberKey {
        private final String m_name;
        private final String m_desc;

        public static MemberKey newConstructorKey(final Constructor method) {
            return new MemberKey(INIT_METHOD_NAME, SignatureHelper.getConstructorSignature(method));
        }

        public static MemberKey newConstructorKey(final String desc) {
            return new MemberKey(INIT_METHOD_NAME, desc);
        }

        public static MemberKey newMethodKey(final Method method) {
            return new MemberKey(method.getName(), SignatureHelper.getMethodSignature(method));
        }

        public static MemberKey newMethodKey(final String name, final String desc) {
            return new MemberKey(name, desc);
        }

        public static MemberKey newFieldKey(final Field field) {
            return new MemberKey(field.getName(), SignatureHelper.getFieldSignature(field));
        }

        public static MemberKey newFieldKey(final String name, final String desc) {
            return new MemberKey(name, desc);
        }

        public MemberKey(final String name, final String desc) {
            m_name = name;
            m_desc = desc;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof MemberKey)) {
                return false;
            }
            final MemberKey memberKey = (MemberKey) o;
            if (m_desc != null ? !m_desc.equals(memberKey.m_desc) : memberKey.m_desc != null) {
                return false;
            }
            if (m_name != null ? !m_name.equals(memberKey.m_name) : memberKey.m_name != null) {
                return false;
            }
            return true;
        }

        public int hashCode() {
            int result;
            result = (m_name != null ? m_name.hashCode() : 0);
            result = 29 * result + (m_desc != null ? m_desc.hashCode() : 0);
            return result;
        }
    }

    /**
     * To be used for debugging purposes.
     */
    private class TraceAnnotationVisitor implements AnnotationVisitor {
        public void visit(final String name, final Object value) {
            System.out.println("    NAMED-VALUE: " + name + "->" + value);
        }

        public void visitEnum(final String name, final String desc, final String value) {
            System.out.println("    ENUM: " + name);
        }

        public AnnotationVisitor visitAnnotation(final String name, final String desc) {
            System.out.println("    ANNOTATION: " + name);
            return new TraceAnnotationVisitor();
        }

        public AnnotationVisitor visitArray(final String name) {
            System.out.println("    ARRAY: " + name);
            return new TraceAnnotationVisitor();
        }

        public void visitEnd() {
        }
    }
}
