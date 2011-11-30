/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.expression;

import com.tc.aspectwerkz.exception.DefinitionException;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The expression namespace as well as a repository for the namespaces. <p/>A namespace is usually defined by the name
 * of the class defining the expression.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public final class ExpressionNamespace {
  /**
   * Namespace container.
   */
  private static final Map s_namespaces = new WeakHashMap();

  /**
   * Map with all the expressions in the namespace, [name:expression] pairs.
   */
  private final Map m_expressions = new HashMap();

  /**
   * The namespace.
   */
  private final String m_namespace;

  /**
   * Creates a new expression namespace.
   *
   * @param namespace
   */
  private ExpressionNamespace(final String namespace) {
    m_namespace = namespace;
  }

  /**
   * Returns the expression namespace for a specific namespace.
   *
   * @param namespace the expression namespace
   * @return the expression namespace abstraction
   */
  public static synchronized ExpressionNamespace getNamespace(final String namespace) {
    if (!s_namespaces.containsKey(namespace)) {
      s_namespaces.put(namespace, new ExpressionNamespace(namespace));
    }
    return (ExpressionNamespace) s_namespaces.get(namespace);
  }

  /**
   * Adds an expression info to the namespace.
   *
   * @param name           the name mapped to the expression
   * @param expressionInfo the expression info to add
   */
  public void addExpressionInfo(final String name, final ExpressionInfo expressionInfo) {
    m_expressions.put(name, expressionInfo);
  }

  /**
   * Returns the expression info with a specific name or null if it could not be found.
   *
   * @param name the name of the expression
   * @return the expression info
   */
  public ExpressionInfo getExpressionInfoOrNull(final String name) {
    int index = name.lastIndexOf('.');
    if (index != -1) {
      // stay in the same CflowStack
      //TODO: allow for lookup in other CflowStack providing they are in the same hierarchy
      return getNamespace(name.substring(0, index)).getExpressionInfoOrNull(
              name.substring(index + 1, name.length())
      );
    } else {
      final ExpressionInfo expressionInfo = ((ExpressionInfo) m_expressions.get(name));
//            if (expressionInfo == null) {
//                throw new DefinitionException(
//                        new StringBuffer().
//                        append("could not resolve reference to pointcut [").
//                        append(name).
//                        append("] in namespace [").
//                        append(m_namespace).
//                        append("]").toString()
//                );
//            }
      return expressionInfo;
    }
  }

  /**
   * Returns the expression info with a specific name or throw an exception if it could not be found.
   *
   * @param name the name of the expression
   * @return the expression info
   */
  public ExpressionInfo getExpressionInfo(final String name) {
    int index = name.lastIndexOf('.');
    if (index != -1) {
      // stay in the same CflowStack
      //TODO: allow for lookup in other CflowStack providing they are in the same hierarchy
      return getNamespace(name.substring(0, index)).getExpressionInfo(name.substring(index + 1, name.length()));
    } else {
      final ExpressionInfo expressionInfo = ((ExpressionInfo) m_expressions.get(name));
      if (expressionInfo == null) {
        throw new DefinitionException(
                new StringBuffer().
                        append("could not resolve reference to pointcut [").
                        append(name).
                        append("] in namespace [").
                        append(m_namespace).
                        append("]").toString()
        );
      }
      return expressionInfo;
    }
  }

  /**
   * Returns the expression with a specific name.
   *
   * @param name the name of the expression
   * @return the expression
   */
  public ExpressionVisitor getExpression(final String name) {
    return getExpressionInfo(name).getExpression();
  }

  /**
   * Returns the advised class expression with a specific name.
   *
   * @param name the name of the expression
   * @return the expression
   */
  public AdvisedClassFilterExpressionVisitor getAdvisedClassExpression(final String name) {
    return getExpressionInfo(name).getAdvisedClassFilterExpression();
  }

  /**
   * Returns the name of the namespace.
   *
   * @return the name of the namespace
   */
  public String getName() {
    return m_namespace;
  }
}