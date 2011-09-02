/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode.aspectwerkz;

import com.tc.aspectwerkz.expression.ExpressionVisitor;

import java.util.Arrays;

import junit.framework.TestCase;

/**
 * Unit test for ExpressionHelper
 */
public class ExpressionHelperTest extends TestCase {

  String[] rawExpressions    = new String[] { "foo", "foo.bar", "foo..bar" };
  String[] withinExpressions = new String[] { "within(foo)", "within(foo.bar)", "within(foo..bar)" };

  public void testCreateExpressionVisitors() {
    ExpressionVisitor[] visitors = new ExpressionHelper().createExpressionVisitors(withinExpressions);
    checkExpressionVisitors(withinExpressions, visitors);
  }

  private void checkExpressionVisitors(String[] expressions, ExpressionVisitor[] visitors) {
    assertNotNull(visitors);
    if (expressions == null) {
      assertEquals(0, visitors.length);
    } else {
      assertEquals(expressions.length, visitors.length);
      for (int i = 0; i < expressions.length; i++) {
        ExpressionVisitor visitor = visitors[i];
        assertNotNull(visitor);
      }
    }
  }

  public void testExpressionPattern2WithinExpressionStringArray() {
    String[] output = ExpressionHelper.expressionPatterns2WithinExpressions(rawExpressions);
    assertTrue(Arrays.equals(withinExpressions, output));
  }

}