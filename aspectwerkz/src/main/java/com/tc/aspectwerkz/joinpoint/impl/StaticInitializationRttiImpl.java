/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint.impl;

import com.tc.aspectwerkz.joinpoint.Rtti;


/**
 * Implementation of static initialization RTTI.
 *
 * @author <a href="mailto:the_mindstorm@evolva.ro">Alex Popescu</a>
 */
public class StaticInitializationRttiImpl implements Rtti {
  private final StaticInitializerSignatureImpl m_signature;

  /**
   * Creates a new staticinitialization RTTI
   *
   * @param signature the underlying <CODE>StaticInitializerSignatureImpl</CODE>
   */
  public StaticInitializationRttiImpl(final StaticInitializerSignatureImpl signature) {
    m_signature = signature;
  }

  /**
   * @see com.tc.aspectwerkz.joinpoint.Rtti#getName()
   */
  public String getName() {
    return m_signature.getName();
  }

  /**
   * @see com.tc.aspectwerkz.joinpoint.Rtti#getTarget()
   */
  public Object getTarget() {
    return null;
  }

  /**
   * @see com.tc.aspectwerkz.joinpoint.Rtti#getThis()
   */
  public Object getThis() {
    return null;
  }

  /**
   * @see com.tc.aspectwerkz.joinpoint.Rtti#getDeclaringType()
   */
  public Class getDeclaringType() {
    return m_signature.getDeclaringType();
  }

  /**
   * @see com.tc.aspectwerkz.joinpoint.Rtti#getModifiers()
   */
  public int getModifiers() {
    return m_signature.getModifiers();
  }

  /**
   * @see com.tc.aspectwerkz.joinpoint.Rtti#cloneFor(java.lang.Object, java.lang.Object)
   */
  public Rtti cloneFor(Object targetInstance, Object thisInstance) {
    return new StaticInitializationRttiImpl(m_signature);
  }

}
