/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.pattern;

import com.tc.aspectwerkz.expression.ExpressionContext;
import com.tc.aspectwerkz.expression.ExpressionVisitor;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.MemberInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.object.bytecode.aspectwerkz.AsmMethodInfo;
import com.tc.object.bytecode.aspectwerkz.ClassInfoFactory;
import com.tc.object.bytecode.aspectwerkz.ExpressionHelper;

public class PatternHelper {
  private static PatternHelper m_helper = new PatternHelper();
  private ExpressionHelper     m_expressionHelper;
  private ClassInfoFactory     m_classInfoFactory;

  public static final PatternHelper getHelper() {
    return m_helper;
  }

  private PatternHelper() {
    m_expressionHelper = new ExpressionHelper();
    m_classInfoFactory = new ClassInfoFactory();
  }

  public ExpressionContext createExecutionExpressionContext(final MemberInfo methodInfo) {
    return m_expressionHelper.createExecutionExpressionContext(methodInfo);
  }

  public boolean matchesMethod(final String expr, final ExpressionContext exprCntx) {
    try {
      String execExpr = ExpressionHelper.expressionPattern2ExecutionExpression(expr);
      ExpressionVisitor visitor = m_expressionHelper.createExpressionVisitor(execExpr);

      return visitor.match(exprCntx);
    } catch (Exception e) {
      return false;
    }
  }

  public boolean matchesMember(final String expr, final MemberInfo methodInfo) {
    return matchesMethod(expr, m_expressionHelper.createExecutionExpressionContext(methodInfo));
  }

  public ExpressionContext createWithinExpressionContext(final String className) {
    return createWithinExpressionContext(m_classInfoFactory.getClassInfo(className));
  }

  public ExpressionContext createWithinExpressionContext(final ClassInfo classInfo) {
    return m_expressionHelper.createWithinExpressionContext(classInfo);
  }

  public boolean matchesClass(final String expr, final ExpressionContext exprCntx) {
    try {
      String withinExpr = ExpressionHelper.expressionPattern2WithinExpression(expr);
      ExpressionVisitor visitor = m_expressionHelper.createExpressionVisitor(withinExpr);

      return visitor.match(exprCntx);
    } catch (Exception e) {
      return false;
    }
  }

  public boolean matchesClass(final String expression, final String className) {
    return matchesClass(expression, m_classInfoFactory.getClassInfo(className));
  }

  public boolean matchesClass(final String expr, final ClassInfo classInfo) {
    return matchesClass(expr, m_expressionHelper.createWithinExpressionContext(classInfo));
  }

  public MethodInfo getMethodInfo(int modifiers, String className, String methodName, String description,
                                  String[] exceptions) {
    return new AsmMethodInfo(m_classInfoFactory, modifiers, className, methodName, description, exceptions);
  }
}
