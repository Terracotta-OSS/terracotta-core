/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining.compiler;

import com.tc.aspectwerkz.expression.ExpressionContext;

/**
 * Holds info sufficient for picking out the join points we are interested in advising.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
final public class MatchingJoinPointInfo {
  private final Class m_joinPointClass;
  private final CompilationInfo m_compilationInfo;
  private final ExpressionContext m_expressionContext;

  public MatchingJoinPointInfo(final Class joinPointClass,
                               final CompilationInfo compilationInfo,
                               final ExpressionContext expressionContext) {
    m_joinPointClass = joinPointClass;
    m_compilationInfo = compilationInfo;
    m_expressionContext = expressionContext;
  }

  public Class getJoinPointClass() {
    return m_joinPointClass;
  }

  public CompilationInfo getCompilationInfo() {
    return m_compilationInfo;
  }

  public ExpressionContext getExpressionContext() {
    return m_expressionContext;
  }

  public int hashCode() {
    return m_compilationInfo.hashCode();
  }

  public boolean equals(Object o) {
    if (! (o instanceof MatchingJoinPointInfo)) {
      return false;
    }
    return ((MatchingJoinPointInfo) o).m_compilationInfo == m_compilationInfo;
  }
}
