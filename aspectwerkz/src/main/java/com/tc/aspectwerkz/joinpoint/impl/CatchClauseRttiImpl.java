/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint.impl;

import com.tc.aspectwerkz.joinpoint.CatchClauseRtti;
import com.tc.aspectwerkz.joinpoint.Rtti;

import java.lang.ref.WeakReference;

/**
 * Implementation for the catch clause RTTI.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public class CatchClauseRttiImpl implements CatchClauseRtti {
  private final CatchClauseSignatureImpl m_signature;

  private WeakReference m_thisRef;

  private WeakReference m_targetRef;

  private Object m_parameterValue;

  /**
   * Creates a new catch clause RTTI.
   *
   * @param signature
   * @param thisInstance
   * @param targetInstance
   */
  public CatchClauseRttiImpl(final CatchClauseSignatureImpl signature,
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
    return new CatchClauseRttiImpl(m_signature, thisInstance, targetInstance);
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
   * Returns the target instance.
   *
   * @return the target instance
   */
  public Object getTarget() {
    return m_targetRef.get();
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
   * Returns the parameter type.
   *
   * @return the parameter type
   */
  public Class getParameterType() {
    return m_signature.getParameterType();
  }

  /**
   * Returns the value of the parameter.
   *
   * @return the value of the parameter
   */
  public Object getParameterValue() {
    return getTarget();//m_parameterValue;
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