/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint.impl;

import com.tc.aspectwerkz.joinpoint.ConstructorRtti;
import com.tc.aspectwerkz.joinpoint.Rtti;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;

/**
 * Implementation for the constructor RTTI.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public class ConstructorRttiImpl implements ConstructorRtti {
  private static final Object[] EMPTY_OBJECT_ARRAY = new Object[]{};

  private final ConstructorSignatureImpl m_signature;

  private WeakReference m_thisRef;

  private WeakReference m_targetRef;

  private Object[] m_parameterValues = EMPTY_OBJECT_ARRAY;

  /**
   * Creates a new constructor RTTI.
   *
   * @param signature
   * @param thisInstance
   * @param targetInstance
   */
  public ConstructorRttiImpl(final ConstructorSignatureImpl signature,
                             final Object thisInstance,
                             final Object targetInstance) {
    m_signature = signature;
    m_thisRef = new WeakReference(thisInstance);
    m_targetRef = new WeakReference(targetInstance);
  }

  /**
   * Clones the RTTI instance.
   *
   * @param thisInstance
   * @param targetInstance
   * @return
   */
  public Rtti cloneFor(final Object thisInstance, final Object targetInstance) {
    return new ConstructorRttiImpl(m_signature, thisInstance, targetInstance);
  }

  /**
   * Returns the target instance.
   *
   * @return the target instance
   */
  public Object getTarget() {
    return m_targetRef.get();
  }

  /**
   * Returns the instance currently executing.
   *
   * @return the instance currently executing
   */
  public Object getThis() {
    return m_thisRef.get();
  }

  /**
   * Returns the constructor.
   *
   * @return the constructor
   */
  public Constructor getConstructor() {
    return m_signature.getConstructor();
  }

  /**
   * Returns the declaring class.
   *
   * @return the declaring class
   */
  public Class getDeclaringType() {
    return m_signature.getDeclaringType();
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
    return m_signature.getModifiers();
  }

  /**
   * Returns the name (f.e. name of method of field).
   *
   * @return
   */
  public String getName() {
    return m_signature.getName();
  }

  /**
   * Returns the exception types declared by the code block.
   *
   * @return the exception types
   */
  public Class[] getExceptionTypes() {
    return m_signature.getExceptionTypes();
  }

  /**
   * Returns the parameter types.
   *
   * @return the parameter types
   */
  public Class[] getParameterTypes() {
    return m_signature.getParameterTypes();
  }

  /**
   * Sets the values of the parameters.
   *
   * @param parameterValues
   */
  public void setParameterValues(final Object[] parameterValues) {
    m_parameterValues = parameterValues;
  }

  /**
   * Returns the values of the parameters.
   *
   * @return the values of the parameters
   */
  public Object[] getParameterValues() {
    return m_parameterValues;
  }

  /**
   * Returns a string representation of the signature.
   *
   * @return a string representation
   * @TODO: implement toString to something meaningful
   */
  public String toString() {
    return super.toString();
  }

}