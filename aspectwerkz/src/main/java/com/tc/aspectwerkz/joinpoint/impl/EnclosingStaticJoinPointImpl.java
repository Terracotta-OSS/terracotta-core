/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint.impl;

import com.tc.aspectwerkz.joinpoint.EnclosingStaticJoinPoint;
import com.tc.aspectwerkz.joinpoint.Signature;
import com.tc.aspectwerkz.joinpoint.management.JoinPointType;

/**
 * Sole implementation of {@link com.tc.aspectwerkz.joinpoint.EnclosingStaticJoinPoint}.
 * It provides access to the enclosing {@link com.tc.aspectwerkz.joinpoint.Signature}
 * of the joinpoint.
 *
 * @author <a href="mailto:the_mindstorm@evolva.ro">Alex Popescu</a>
 */
public class EnclosingStaticJoinPointImpl implements EnclosingStaticJoinPoint {
  private Signature m_signature;
  private JoinPointType m_joinPointType;

  public EnclosingStaticJoinPointImpl(Signature signature, JoinPointType jpType) {
    m_signature = signature;
    m_joinPointType = jpType;
  }

  /**
   * Retrieve the {@link Signature} of the enclosing join point.
   *
   * @return a {@link Signature}
   */
  public Signature getSignature() {
    return m_signature;
  }

  /**
   * Return a join point type corresponding to the enclosing join point.
   *
   * @return one of {@link JoinPointType#CONSTRUCTOR_EXECUTION} or
   *         {@link JoinPointType#METHOD_EXECUTION} or {@link JoinPointType#STATIC_INITIALIZATION}.
   */
  public JoinPointType getType() {
    return m_joinPointType;
  }
}
