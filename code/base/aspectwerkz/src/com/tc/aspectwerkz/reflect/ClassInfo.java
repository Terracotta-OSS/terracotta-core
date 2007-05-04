/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.reflect;

import com.tc.backport175.bytecode.AnnotationReader;
import com.tc.backport175.bytecode.AnnotationElement;

/**
 * Interface for the class info implementations.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public interface ClassInfo extends ReflectionInfo {

  final static AnnotationElement.Annotation[] EMPTY_ANNOTATION_ARRAY = new AnnotationElement.Annotation[0];

  /**
   * Returns a constructor info by its hash.
   * Looks up in the hierarchy
   *
   * @param hash
   * @return
   */
  ConstructorInfo getConstructor(int hash);

  /**
   * Returns the constructors info.
   * Does not looks up in the hierarchy
   *
   * @return the constructors info
   */
  ConstructorInfo[] getConstructors();

  /**
   * Returns a method info by its hash.
   * Looks up in the hierarchy
   *
   * @param hash
   * @return
   */
  MethodInfo getMethod(int hash);

  /**
   * Returns the methods info.
   * Does not looks up in the hierarchy
   *
   * @return the methods info
   */
  MethodInfo[] getMethods();

  /**
   * Returns a field info by its hash.
   * Looks up in the hierarchy
   *
   * @param hash
   * @return
   */
  FieldInfo getField(int hash);

  /**
   * Returns the fields info.
   * Does not looks up in the hierarchy
   *
   * @return the fields info
   */
  FieldInfo[] getFields();

  /**
   * Returns the class loader that loaded this class.
   *
   * @return the class loader
   */
  ClassLoader getClassLoader();

  /**
   * Checks if the class has a static initalizer.
   *
   * @return
   */
  boolean hasStaticInitializer();

  /**
   * Returns the static initializer info of the current underlying class if any.
   *
   * @return
   */
  StaticInitializationInfo staticInitializer();

  /**
   * Returns the interfaces.
   *
   * @return the interfaces
   */
  ClassInfo[] getInterfaces();

  /**
   * Returns the super class, or null (superclass of java.lang.Object)
   *
   * @return the super class
   */
  ClassInfo getSuperclass();

  /**
   * Returns the component type if array type else null.
   *
   * @return the component type
   */
  ClassInfo getComponentType();

  /**
   * Is the class an interface.
   *
   * @return
   */
  boolean isInterface();

  /**
   * Is the class a primitive type.
   *
   * @return
   */
  boolean isPrimitive();

  /**
   * Is the class an array type.
   *
   * @return
   */
  boolean isArray();

  /**
   * @return
   */
  AnnotationReader getAnnotationReader();

  public static class NullClassInfo implements ClassInfo {

    public ConstructorInfo getConstructor(int hash) {
      return null;
    }

    public ConstructorInfo[] getConstructors() {
      return new ConstructorInfo[0];
    }

    public MethodInfo getMethod(int hash) {
      return null;
    }

    public MethodInfo[] getMethods() {
      return new MethodInfo[0];
    }

    public FieldInfo getField(int hash) {
      return null;
    }

    public FieldInfo[] getFields() {
      return new FieldInfo[0];
    }

    public boolean hasStaticInitializer() {
      return false;
    }

    /**
     * @see com.tc.aspectwerkz.reflect.ClassInfo#staticInitializer()
     */
    public StaticInitializationInfo staticInitializer() {
      return null;
    }

    public ClassInfo[] getInterfaces() {
      return new ClassInfo[0];
    }

    public ClassInfo getSuperclass() {
      return null;
    }

    public ClassLoader getClassLoader() {
      return null;
    }

    public ClassInfo getComponentType() {
      return null;
    }

    public boolean isInterface() {
      return false;
    }

    public boolean isPrimitive() {
      return false;
    }

    public boolean isArray() {
      return false;
    }

    public String getName() {
      return "__UNKNOWN__";
    }

    public String getSignature() {
      return null;
    }
    
    public String getGenericsSignature() {
      return null;
    }

    public int getModifiers() {
      return 0;
    }

    public AnnotationElement.Annotation[] getAnnotations() {
      return EMPTY_ANNOTATION_ARRAY;
    }

    public AnnotationReader getAnnotationReader() {
      return null;
    }
  }
}

