/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import com.tc.aspectwerkz.expression.ExpressionContext;
import com.tc.aspectwerkz.expression.ExpressionVisitor;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.object.bytecode.aspectwerkz.ExpressionHelper;

public class ClassExpressionMatcherImpl implements ClassExpressionMatcher {

  private final ExpressionHelper expressionHelper;
  private ExpressionVisitor expressionVisitor;

  public ClassExpressionMatcherImpl(ExpressionHelper expressionHelper, String classExpression) {
    this.expressionVisitor = expressionHelper.createExpressionVisitor( //
        ExpressionHelper.expressionPattern2WithinExpression(classExpression));
    this.expressionHelper = expressionHelper;
  }

  public boolean match(ClassInfo classInfo) {
    ExpressionContext ctxt = expressionHelper.createWithinExpressionContext(classInfo);
    return expressionVisitor.match(ctxt);
  }

}
