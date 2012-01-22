/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.definition;

import com.tc.aspectwerkz.transform.TransformationConstants;
import com.tc.aspectwerkz.expression.ExpressionInfo;

/**
 * Represents a deployment scope pointcut expression, that is used by the system to "prepare" the
 * join points that are picked out by this pointcut. Needed to allow hot-deployment of aspects
 * in a safe and predictable way.
 * <p/>
 * Can not and should not be created by the user only given to him from the framework.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public final class DeploymentScope {

  private final String m_name;
  private final String m_expression;
  /**
   * System prepared pointcut that matches all.
   */
  public static final DeploymentScope MATCH_ALL = new DeploymentScope(
          TransformationConstants.ASPECTWERKZ_PREFIX + "DeploymentScopes",
          "within(*..*)"
  );

  /**
   * Creates a new pointcut, should only be created by the system.
   *
   * @param name
   * @param expression
   */
  DeploymentScope(final String name, final String expression) {
    m_name = name;
    m_expression = expression;
  }

  /**
   * Returns the name of the pointcut.
   *
   * @return
   */
  public String getName() {
    return m_name;
  }

  /**
   * Returns the expression as a string.
   *
   * @return
   */
  public String getExpression() {
    return m_expression;
  }

  /**
   * Merges the scope expression with a new expression. Uses '&&' to merge them.
   *
   * @param expression
   * @return
   */
  public ExpressionInfo newExpressionInfo(final ExpressionInfo expression) {
    return new ExpressionInfo(
            new StringBuffer().
                    append('(').
                    append(expression.toString()).
                    append(')').
                    append(" && ").
                    append(m_expression).
                    toString(),
            expression.getNamespace()
    );
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DeploymentScope)) {
      return false;
    }

    final DeploymentScope deploymentScope = (DeploymentScope) o;

    if (!m_expression.equals(deploymentScope.m_expression)) {
      return false;
    }
    if (!m_name.equals(deploymentScope.m_name)) {
      return false;
    }

    return true;
  }

  public int hashCode() {
    int result;
    result = m_name.hashCode();
    result = 29 * result + m_expression.hashCode();
    return result;
  }
}
