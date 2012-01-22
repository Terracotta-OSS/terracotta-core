/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.definition;


import com.tc.aspectwerkz.expression.ExpressionInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the meta-data for the interface introductions. <p/>This definition holds only pure interface introduction.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 * @author <a href="mailto:alex@gnilux.com">Alexandre Vasseur </a>
 */
public class InterfaceIntroductionDefinition {
  /**
   * The name of the interface introduction.
   */
  protected final String m_name;

  /**
   * The introduction expressions.
   */
  protected ExpressionInfo[] m_expressionInfos = new ExpressionInfo[]{};

  /**
   * The attribute for the introduction.
   */
  protected String m_attribute = "";

  /**
   * The interface classes name.
   */
  protected List m_interfaceClassNames = new ArrayList();

  /**
   * Creates a new introduction meta-data instance.
   *
   * @param name               the name of the expressionInfo
   * @param interfaceClassName the class name of the interface
   */
  public InterfaceIntroductionDefinition(final String name, final String interfaceClassName) {
    if (name == null) {
      throw new IllegalArgumentException("name can not be null");
    }
    if (interfaceClassName == null) {
      throw new IllegalArgumentException("interface class name can not be null");
    }
    m_name = name;
    m_interfaceClassNames.add(interfaceClassName);
  }

  /**
   * Returns the name of the introduction.
   *
   * @return the name
   */
  public String getName() {
    return m_name;
  }

  /**
   * Returns the expressions.
   *
   * @return the expressions array
   */
  public ExpressionInfo[] getExpressionInfos() {
    return m_expressionInfos;
  }

  /**
   * Returns the class name of the interface.
   *
   * @return the class name of the interface
   */
  public String getInterfaceClassName() {
    return (String) m_interfaceClassNames.get(0);
  }

  /**
   * Returns the class name of the interface.
   *
   * @return the class name of the interface
   */
  public List getInterfaceClassNames() {
    return m_interfaceClassNames;
  }

  /**
   * Returns the attribute.
   *
   * @return the attribute
   */
  public String getAttribute() {
    return m_attribute;
  }

  /**
   * Sets the attribute.
   *
   * @param attribute the attribute
   */
  public void setAttribute(final String attribute) {
    m_attribute = attribute;
  }

  /**
   * Adds a new expression info.
   *
   * @param expression a new expression info
   */
  public void addExpressionInfo(final ExpressionInfo expression) {
    final ExpressionInfo[] tmpExpressions = new ExpressionInfo[m_expressionInfos.length + 1];
    java.lang.System.arraycopy(m_expressionInfos, 0, tmpExpressions, 0, m_expressionInfos.length);
    tmpExpressions[m_expressionInfos.length] = expression;
    m_expressionInfos = new ExpressionInfo[m_expressionInfos.length + 1];
    java.lang.System.arraycopy(tmpExpressions, 0, m_expressionInfos, 0, tmpExpressions.length);
  }

  /**
   * Adds an array with new expression infos.
   *
   * @param expressions an array with new expression infos
   */
  public void addExpressionInfos(final ExpressionInfo[] expressions) {
    final ExpressionInfo[] tmpExpressions = new ExpressionInfo[m_expressionInfos.length + expressions.length];
    java.lang.System.arraycopy(m_expressionInfos, 0, tmpExpressions, 0, m_expressionInfos.length);
    java.lang.System.arraycopy(expressions, 0, tmpExpressions, m_expressionInfos.length, expressions.length);
    m_expressionInfos = new ExpressionInfo[m_expressionInfos.length + expressions.length];
    java.lang.System.arraycopy(tmpExpressions, 0, m_expressionInfos, 0, tmpExpressions.length);
  }
}
