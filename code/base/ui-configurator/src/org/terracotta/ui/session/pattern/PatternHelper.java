/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session.pattern;

import com.tc.aspectwerkz.expression.ExpressionContext;
import com.tc.aspectwerkz.expression.ExpressionVisitor;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.MemberInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.object.bytecode.aspectwerkz.AsmMethodInfo;
import com.tc.object.bytecode.aspectwerkz.ClassInfoFactory;
import com.tc.object.bytecode.aspectwerkz.ExpressionHelper;

public class PatternHelper {
  private static final PatternHelper helper = new PatternHelper();
  private ExpressionHelper           expressionHelper;
  private ClassInfoFactory           classInfoFactory;

  public static final PatternHelper getHelper() {
    return helper;
  }

  private PatternHelper() {
    expressionHelper = new ExpressionHelper();
    classInfoFactory = new ClassInfoFactory();
  }

  public ExpressionContext createExecutionExpressionContext(final MemberInfo methodInfo) {
    return expressionHelper.createExecutionExpressionContext(methodInfo);
  }

  public boolean matchesMethod(final String expr, final ExpressionContext exprCntx) {
    try {
      String execExpr = ExpressionHelper.expressionPattern2ExecutionExpression(expr);
      ExpressionVisitor visitor = expressionHelper.createExpressionVisitor(execExpr);
      return visitor.match(exprCntx);
    } catch (Exception e) {
      return false;
    }
  }

  public boolean matchesMember(final String expr, final MemberInfo methodInfo) {
    return matchesMethod(expr, expressionHelper.createExecutionExpressionContext(methodInfo));
  }

  public ExpressionContext createWithinExpressionContext(final String className) {
    return createWithinExpressionContext(classInfoFactory.getClassInfo(className));
  }

  public ExpressionContext createWithinExpressionContext(final ClassInfo classInfo) {
    return expressionHelper.createWithinExpressionContext(classInfo);
  }

  public boolean matchesClass(final String expr, final ExpressionContext exprCntx) {
    try {
      String withinExpr = ExpressionHelper.expressionPattern2WithinExpression(expr);
      ExpressionVisitor visitor = expressionHelper.createExpressionVisitor(withinExpr);
      return visitor.match(exprCntx);
    } catch (Exception e) {
      return false;
    }
  }

  public boolean matchesClass(final String expression, final String className) {
    return matchesClass(expression, classInfoFactory.getClassInfo(className));
  }

  public boolean matchesClass(final String expr, final ClassInfo classInfo) {
    return matchesClass(expr, expressionHelper.createWithinExpressionContext(classInfo));
  }

  public MethodInfo getMethodInfo(int modifiers, String className, String methodName, String description,
                                  String[] exceptions) {
    return new AsmMethodInfo(classInfoFactory, modifiers, className, methodName, description, exceptions);
  }
}
