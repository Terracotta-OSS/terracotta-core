/*******************************************************************************************
 * Copyright (c) Jonas Bonér, Alexandre Vasseur. All rights reserved.                      *
 * http://backport175.codehaus.org                                                         *
 * --------------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of Apache License Version 2.0 *
 * a copy of which has been included with this distribution in the license.txt file.       *
 *******************************************************************************************/
package com.tc.backport175.bytecode;

import com.tc.backport175.ReaderException;
import com.tc.backport175.bytecode.AnnotationElement.Annotation;

import com.tc.asm.AnnotationVisitor;
import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassReader;
import com.tc.asm.ClassVisitor;
import com.tc.asm.ClassWriter;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Container for default value of the annotations
 * <p/>
 * As per spec, default values are "unnamed" annotation on the element method of the annotation interface.
 *
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
class AnnotationDefaults {

    /**
     * Cache of default values, key is annotationClassKey, value is Annotation whose elements are named according
     * to the element name which have a default value. Element without default value are thus not present in the Annotation.
     */
    private static Map s_annotationDefaults = new WeakHashMap();

    /**
     * Retrieve (create if not in cache) the annotation defaults
     *
     * @param annotationClassName
     * @param loader
     * @return
     */
    public static AnnotationElement.Annotation getDefaults(final String annotationClassName, final ClassLoader loader) {
        AnnotationReader.ClassKey key = new AnnotationReader.ClassKey(annotationClassName, loader);
        AnnotationElement.Annotation defaults = (AnnotationElement.Annotation) s_annotationDefaults.get(key);
        if (defaults == null) {
            final AnnotationElement.Annotation newDefaults = new AnnotationElement.Annotation(annotationClassName);
            final byte[] bytes;
            try {
                bytes = AnnotationReader.getBytecodeFor(annotationClassName, loader);
            } catch (Exception e) {
                throw new ReaderException("could not retrieve the bytecode from the bytecode provider for class [" + annotationClassName+ "]", e);
            }
            ClassReader cr = new ClassReader(bytes);
            ClassWriter cw = new ClassWriter(false, true);
            cr.accept(new AnnotationDefaultsClassVisitor(cw, newDefaults, loader), true);
            defaults = newDefaults;
            s_annotationDefaults.put(key, newDefaults);
        }
        return defaults;
    }

    private static final class AnnotationDefaultsClassVisitor extends ClassAdapter {

      private final AnnotationElement.Annotation  defaults;
      private final ClassLoader loader;

      private AnnotationDefaultsClassVisitor(ClassVisitor cv, AnnotationElement.Annotation defaults, ClassLoader loader) {
        super(cv);
        this.defaults = defaults;
        this.loader = loader;
      }

      public MethodVisitor visitMethod(int access, final String name, String desc, String signature, String[] exceptions) {
          return new AnnotationDefaultsMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions), name, defaults, loader);
      }
    }

    private static final class AnnotationDefaultsMethodVisitor extends MethodAdapter {
      private final String name;
      private final Annotation defaults;
      private final ClassLoader loader;
      
      private AnnotationDefaultsMethodVisitor(MethodVisitor mv, String name, Annotation defaults, ClassLoader loader) {
        super(mv);
        this.name = name;
        this.defaults = defaults;
        this.loader = loader;
      }
      
      public AnnotationVisitor visitAnnotationDefault() {
        return new DefaultAnnotationBuilderVisitor(defaults, name, loader);
      }
    }

    /**
     * Read the default value of annotation element
     * Behave like a regular annotation visitor except that the name is force to the element name (else null in bytecode)
     *
     * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
     */
    static class DefaultAnnotationBuilderVisitor extends AnnotationReader.AnnotationBuilderVisitor {

        private String m_methodName;

        public DefaultAnnotationBuilderVisitor(final AnnotationElement.NestedAnnotationElement annotation, String methodName, ClassLoader loader) {
            super(annotation, loader, null);
            m_methodName = methodName;
        }

        public void visit(String name, Object value) {
            super.visit(m_methodName, value);
        }

        public void visitEnum(String name, String desc, String value) {
            super.visitEnum(m_methodName, desc, value);
        }

        public AnnotationVisitor visitAnnotation(String name, String desc) {
            return super.visitAnnotation(m_methodName, desc);
        }

        public AnnotationVisitor visitArray(String name) {
            return super.visitArray(m_methodName);
        }

    }

    public static void refresh(AnnotationReader.ClassKey key) {
        AnnotationElement.Annotation defaults = (AnnotationElement.Annotation) s_annotationDefaults.get(key);
        if (defaults != null) {
            s_annotationDefaults.remove(key);
        }
    }
}
