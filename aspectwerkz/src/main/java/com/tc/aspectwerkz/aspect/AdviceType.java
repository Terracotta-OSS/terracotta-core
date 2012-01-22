/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.aspect;

import java.io.Serializable;

/**
 * Type-safe enum for the advice types.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public class AdviceType implements Serializable {

  public static final AdviceType AROUND = new AdviceType("around");
  public static final AdviceType BEFORE = new AdviceType("before");
  public static final AdviceType AFTER = new AdviceType("after");
  public static final AdviceType AFTER_FINALLY = new AdviceType("afterFinally");
  public static final AdviceType AFTER_RETURNING = new AdviceType("afterReturning");
  public static final AdviceType AFTER_THROWING = new AdviceType("afterThrowing");

  private final String m_name;

  private AdviceType(String name) {
    m_name = name;
  }

  public String toString() {
    return m_name;
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AdviceType)) {
      return false;
    }
    final AdviceType adviceType = (AdviceType) o;
    if ((m_name != null) ? (!m_name.equals(adviceType.m_name)) : (adviceType.m_name != null)) {
      return false;
    }
    return true;
  }

  public int hashCode() {
    return ((m_name != null) ? m_name.hashCode() : 0);
  }
}
