/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.definition;

/**
 * Holds the meta-data for the pointcuts.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public class PointcutDefinition {
  /**
   * The expression.
   */
  private final String m_expression;

  /**
   * Creates a new pointcut definition instance.
   *
   * @param expression
   */
  public PointcutDefinition(final String expression) {
    m_expression = expression;
  }

  /**
   * Returns the expression for the pointcut.
   *
   * @return the expression for the pointcut
   */
  public String getExpression() {
    return m_expression;
  }
}