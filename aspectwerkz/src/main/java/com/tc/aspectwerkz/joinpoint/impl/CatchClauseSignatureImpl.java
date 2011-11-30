/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint.impl;

import com.tc.aspectwerkz.joinpoint.CatchClauseSignature;
import com.tc.aspectwerkz.joinpoint.Signature;

/**
 * Implementation for the catch clause signature.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public class CatchClauseSignatureImpl implements CatchClauseSignature {

  private Class m_exceptionType;

  /**
   * Creates a new catch clause signature.
   *
   * @param exceptionClass
   */
  public CatchClauseSignatureImpl(final Class exceptionClass) {
    m_exceptionType = exceptionClass;
  }

  /**
   * Returns the exception class.
   *
   * @return the declaring class
   */
  public Class getDeclaringType() {
    return m_exceptionType;
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
    return m_exceptionType.getModifiers();
  }

  /**
   * Returns the name
   *
   * @return the name
   */
  public String getName() {
    return m_exceptionType.getName();
  }

  /**
   * Returns the exception type.
   *
   * @return the parameter type
   */
  public Class getParameterType() {
    return m_exceptionType;
  }

  /**
   * Returns a string representation of the signature.
   *
   * @return a string representation
   */
  public String toString() {
    return getName();
  }

  /**
   * Creates a deep copy of the signature.
   *
   * @return a deep copy of the signature
   */
  public Signature newInstance() {
    return new CatchClauseSignatureImpl(m_exceptionType);
  }
}