/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode.aspectwerkz;

import com.tc.aspectwerkz.expression.ExpressionContext;
import com.tc.aspectwerkz.expression.ExpressionVisitor;
import com.tc.aspectwerkz.reflect.ClassInfo;

import junit.framework.TestCase;

/**
 *
 */
public class ExpressionInfoTest extends TestCase {

  private ClassInfo classInfo;

  // private SimpleMethodInfo methodInfo;

  public void setUp() throws Exception {
    ClassInfoFactory classInfoFactory = new ClassInfoFactory();
    classInfo = classInfoFactory.getClassInfo(com.tc.object.bytecode.aspectwerkz.Target.class.getName());
  }

  // TODO: Figure out how to create a MethodInfo class out of an ASM ClassVisitor context.
  //
  // public void testWithinMethod() throws Exception {
  // ExpressionContext ctxt = new ExpressionContext(PointcutType.EXECUTION, methodInfo, methodInfo);
  //
  // String expression = "execution(public void com.tc.bytecode.aspectwerkz.Target.testMethod(..))";
  // assertTrue(checkMatch(expression, ctxt));
  // }

  public void testWithinClass() throws Exception {
    ExpressionHelper eh = new ExpressionHelper();
    ExpressionContext ctxt = eh.createWithinExpressionContext(this.classInfo);

    String expression = ExpressionHelper.expressionPattern2WithinExpression("com.tc.object.bytecode.aspectwerkz.*");
    assertTrue(checkMatch(expression, ctxt));

    expression = ExpressionHelper.expressionPattern2WithinExpression("com.tc.object.bytecode.*");
    assertFalse(checkMatch(expression, ctxt));

    expression = ExpressionHelper.expressionPattern2WithinExpression("com.tc.object.bytecode..*");
    assertTrue(checkMatch(expression, ctxt));

    expression = ExpressionHelper.expressionPattern2WithinExpression("java.lang..*");
    assertFalse(checkMatch(expression, ctxt));

    expression = ExpressionHelper.expressionPattern2WithinExpression("*.tc..*");
    assertTrue(checkMatch(expression, ctxt));

    expression = ExpressionHelper.expressionPattern2WithinExpression("*.object.bytecode.aspectwerkz.*");
    assertFalse(checkMatch(expression, ctxt));

    expression = ExpressionHelper.expressionPattern2WithinExpression("*..object.bytecode.aspectwerkz.*");
    assertTrue(checkMatch(expression, ctxt));

    expression = ExpressionHelper.expressionPattern2WithinExpression("*.*.object.bytecode..*");
    assertTrue(checkMatch(expression, ctxt));
  }

  private boolean checkMatch(String expression, ExpressionContext ctxt) {
    ExpressionVisitor visitor = new ExpressionHelper().createExpressionVisitor(expression);
    return visitor.match(ctxt);
  }
}