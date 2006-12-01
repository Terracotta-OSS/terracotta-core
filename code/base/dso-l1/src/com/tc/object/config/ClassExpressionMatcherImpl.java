/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import com.tc.aspectwerkz.expression.ExpressionContext;
import com.tc.aspectwerkz.expression.ExpressionVisitor;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.object.bytecode.aspectwerkz.ClassInfoFactory;
import com.tc.object.bytecode.aspectwerkz.ExpressionHelper;

public class ClassExpressionMatcherImpl implements ClassExpressionMatcher {

  private final ClassInfoFactory classInfoFactory;
  private final ExpressionHelper expressionHelper;
  private ExpressionVisitor expressionVisitor;

  public ClassExpressionMatcherImpl(ClassInfoFactory classInfoFactory, ExpressionHelper expressionHelper, String classExpression) {
    this.expressionVisitor = expressionHelper.createExpressionVisitor(ExpressionHelper
                                                   .expressionPattern2WithinExpression(classExpression));
    this.classInfoFactory = classInfoFactory;
    this.expressionHelper = expressionHelper;
  }

  public boolean match(String className) {
    ClassInfo classInfo = classInfoFactory.getClassInfo(className);
    ExpressionContext ctxt = expressionHelper.createWithinExpressionContext(classInfo);
    return expressionVisitor.match(ctxt);
  }

}
