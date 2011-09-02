/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.cflow;


import com.tc.aspectwerkz.aspect.AdviceType;
import com.tc.aspectwerkz.definition.AdviceDefinition;
import com.tc.aspectwerkz.definition.AspectDefinition;
import com.tc.aspectwerkz.definition.SystemDefinition;
import com.tc.aspectwerkz.reflect.impl.java.JavaClassInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.expression.ExpressionInfo;

import java.util.List;
import java.util.ArrayList;

/**
 * A Cflow binding represents an extracted cflow or cflowbelow subexpression
 * <p/>
 * For a given pointcut "pcA and cflowA or cflowbelowB", we will extract two bindings.
 * The m_cflowID must be unique on a per cflow sub expresion basis ie JVM wide.
 * <p/>
 * Note: CflowBinding hashcode depends on Cflow_ID (sub expr) + isCflowBelow only.
 *
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public class CflowBinding {

  /**
   * the base implementation that hosts the cflow advices
   */
  private final static ClassInfo ABSTRACT_CFLOWCLASS = JavaClassInfo.getClassInfo(AbstractCflowSystemAspect.class);
  private final static MethodInfo CFLOW_ENTER_ADVICE;
  private final static MethodInfo CFLOW_EXIT_ADVICE;

  static {
    MethodInfo enter = null;
    MethodInfo exit = null;
    for (int i = 0; i < ABSTRACT_CFLOWCLASS.getMethods().length; i++) {
      MethodInfo methodInfo = ABSTRACT_CFLOWCLASS.getMethods()[i];
      if (methodInfo.getName().equals("enter")) {
        enter = methodInfo;
      } else if (methodInfo.getName().equals("exit")) {
        exit = methodInfo;
      }
    }
    if (enter == null || exit == null) {
      throw new Error("Could not gather cflow advices from " + AbstractCflowSystemAspect.class);
    } else {
      CFLOW_ENTER_ADVICE = enter;
      CFLOW_EXIT_ADVICE = exit;
    }
  }

  /**
   * cflow unique id
   */
  private int m_cflowID;

  /**
   * pointcut that represents this cflow sub-expression
   */
  private ExpressionInfo m_cflowSubExpression;

  /**
   * pointcut that represents the containing expression
   */
  private ExpressionInfo m_outerExpression;

  /**
   * marker if this binding is a cflow below, not used at the moment
   */
  private boolean m_isCflowBelow;

  /**
   * Cosntructs a new cflow binding
   *
   * @param cflowID
   * @param cflowSubExpression
   * @param isCflowBelow
   */
  public CflowBinding(int cflowID, ExpressionInfo cflowSubExpression, ExpressionInfo outerExpression, boolean isCflowBelow) {
    m_cflowID = cflowID;
    m_cflowSubExpression = cflowSubExpression;
    m_outerExpression = outerExpression;
    m_isCflowBelow = isCflowBelow;
  }

  /**
   * @return the sub expression
   */
  public ExpressionInfo getExpression() {
    return m_cflowSubExpression;
  }

  /**
   * Extract the cflow bindings from any pointcut
   * This includes both cflow and cflowbelow
   *
   * @param expressionInfo the pointcut expression frow where to extract the cflow bindings
   * @return a list of CflowBinding, can be empty
   */
  public static List getCflowBindingsForCflowOf(ExpressionInfo expressionInfo) {
    List cflowBindings = new ArrayList();
    if (expressionInfo != null) {
      expressionInfo.getCflowAspectExpression().populateCflowAspectBindings(cflowBindings);
    }
    return cflowBindings;
  }

  /**
   * Create an aspect definition for this cflow binding in the given system.
   * The cflow jit aspects will gets compiled and loaded
   *
   * @param systemDefinition
   * @param loader
   * @return the cflow aspect definition
   */
  public AspectDefinition getAspectDefinition(SystemDefinition systemDefinition, ClassLoader loader) {
    String aspectName = CflowCompiler.getCflowAspectClassName(m_cflowID);

    // check if we have already register this aspect
    // TODO: it may happen that the aspect gets register somewhere up in the hierarchy ??
    // it is optim only

    // TODO: how to do this class define lazyly and not pass in a classloader ?
    // could be done in the JIT jp clinit when 1+ advice has a cflow binding
    Class aspectClass = CflowCompiler.compileCflowAspectAndAttachToClassLoader(loader, m_cflowID);
    ClassInfo cflowAspectInfo = JavaClassInfo.getClassInfo(aspectClass);

    AspectDefinition aspectDef = new AspectDefinition(
            aspectName.replace('/', '.'),
            cflowAspectInfo,
            systemDefinition
    );
    aspectDef.addBeforeAdviceDefinition(
            new AdviceDefinition(
                    CFLOW_ENTER_ADVICE.getName(),
                    AdviceType.BEFORE,
                    null,
                    aspectName,
                    aspectName,
                    m_cflowSubExpression,
                    CFLOW_ENTER_ADVICE,
                    aspectDef
            )
    );
    aspectDef.addAfterAdviceDefinition(
            new AdviceDefinition(
                    CFLOW_EXIT_ADVICE.getName(),
                    AdviceType.AFTER_FINALLY,
                    null,
                    aspectName,
                    aspectName,
                    m_cflowSubExpression,
                    CFLOW_EXIT_ADVICE,
                    aspectDef
            )
    );

    return aspectDef;
  }

  public boolean isCflowBelow() {
    return m_isCflowBelow;
  }

  public int getCflowID() {
    return m_cflowID;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CflowBinding)) return false;

    final CflowBinding cflowBinding = (CflowBinding) o;

    if (m_cflowID != cflowBinding.m_cflowID) return false;
    if (m_isCflowBelow != cflowBinding.m_isCflowBelow) return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = m_cflowID;
    result = 29 * result + (m_isCflowBelow ? 1 : 0);
    return result;
  }
}
