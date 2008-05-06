/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.reflect;

import com.tc.backport175.bytecode.AnnotationElement.Annotation;

/**
 * Base interface for the reflection info hierarchy.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bon�r </a>
 */
public interface ReflectionInfo {

  /**
   * Returns the name element.
   * If the element is an array class, its name is as a human writes it: java.lang.String[]
   *
   * @return the name of the element
   */
  String getName();

  /**
   * Returns the signature for the element.
   *
   * @return the signature for the element
   */
  String getSignature();

  /**
   * Returns the internal generics signature for the element.
   *
   * @return the internal generics signature for the element
   */
  String getGenericsSignature();
  
  /**
   * Returns the class modifiers.
   *
   * @return the class modifiers
   */
  int getModifiers();

  /**
   * Returns the annotations.
   *
   * @return the annotations
   */
  Annotation[] getAnnotations();
}