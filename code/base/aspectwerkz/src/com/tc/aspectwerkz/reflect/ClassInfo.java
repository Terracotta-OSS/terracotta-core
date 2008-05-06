/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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


}

