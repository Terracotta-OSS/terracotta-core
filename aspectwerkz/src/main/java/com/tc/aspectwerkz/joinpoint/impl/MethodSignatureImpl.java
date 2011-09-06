/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint.impl;

import com.tc.backport175.Annotation;
import com.tc.backport175.Annotations;

import com.tc.aspectwerkz.joinpoint.MethodSignature;

import java.lang.reflect.Method;

/**
 * Implementation for the method signature.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public class MethodSignatureImpl implements MethodSignature {
  private final Class m_declaringType;

  private final Method m_method;

  /**
   * @param declaringType
   * @param method
   */
  public MethodSignatureImpl(final Class declaringType, final Method method) {
    m_declaringType = declaringType;
    m_method = method;
  }

  /**
   * Returns the method.
   *
   * @return the method
   */
  public Method getMethod() {
    return m_method;
  }

  /**
   * Returns the declaring class.
   *
   * @return the declaring class
   */
  public Class getDeclaringType() {
    return m_declaringType;
  }

  /**
   * Returns the modifiers for the signature. <p/>Could be used like this:
   * <p/>
   * <pre>
   * boolean isPublic = java.lang.reflect.Modifier.isPublic(signature.getModifiers());
   * </pre>
   *
   * @return the mofifiers
   */
  public int getModifiers() {
    return m_method.getModifiers();
  }

  /**
   * Returns the name (f.e. name of method of field).
   *
   * @return
   */
  public String getName() {
    return m_method.getName();
  }

  /**
   * Returns the exception types declared by the code block.
   *
   * @return the exception types
   */
  public Class[] getExceptionTypes() {
    return m_method.getExceptionTypes();
  }

  /**
   * Returns the parameter types.
   *
   * @return the parameter types
   */
  public Class[] getParameterTypes() {
    return m_method.getParameterTypes();
  }

  /**
   * Returns the return type.
   *
   * @return the return type
   */
  public Class getReturnType() {
    return m_method.getReturnType();
  }

  /**
   * Return the annotation with a specific class.
   *
   * @param annotationClass the annotation class
   * @return the annotation or null
   */
  public Annotation getAnnotation(final Class annotationClass) {
    return Annotations.getAnnotation(annotationClass, m_method);
  }

  /**
   * Return all the annotations.
   *
   * @return annotations
   */
  public Annotation[] getAnnotations() {
    return Annotations.getAnnotations(m_method);
  }

  /**
   * Returns a string representation of the signature.
   *
   * @return a string representation
   */
  public String toString() {
    return m_method.toString();
  }
}