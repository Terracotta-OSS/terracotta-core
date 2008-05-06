/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode.aspectwerkz;

import com.tc.aspectwerkz.expression.ExpressionVisitor;
import com.tc.aspectwerkz.expression.ExpressionInfo;
import com.tc.aspectwerkz.expression.ExpressionContext;
import com.tc.aspectwerkz.expression.PointcutType;
import com.tc.aspectwerkz.reflect.MemberInfo;
import com.tc.aspectwerkz.reflect.ClassInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for dealing with Aspectwerkz expressions
 */
public class ExpressionHelper {

  private final Map expressionInfoCache = new HashMap();

  public ExpressionVisitor[] createExpressionVisitors(String[] expressions) {
    ExpressionVisitor[] rv = null;
    if (expressions == null) {
      rv = new ExpressionVisitor[] {};
    } else {
      rv = new ExpressionVisitor[expressions.length];
      for (int i = 0; i < expressions.length; i++) {
        rv[i] = createExpressionVisitor(expressions[i]);
      }
    }
    return rv;
  }

  /**
   * Creates and returns ExpressionVisitor. An ExpressionVisitor can be used to match ClassInfo, MethodInfo, etc.
   * against.
   */
  public ExpressionVisitor createExpressionVisitor(String expression) {
    return createExpressionInfo(expression).getExpression();
  }

  /**
   * Creates and returns ExpressionInfo from the given expression. <p/>Since the expression namespace doesn't appear to
   * be used, this is probably the version of this method you're looking for.
   */
  public ExpressionInfo createExpressionInfo(String expression) {
    return createExpressionInfo(expression, "__tc_default");
  }

  /**
   * Creates and returns ExpressionInfo object from the given expression and namespace. <p/>I'm not sure what namespace
   * is for. As far as I can tell from examining the aspectwerkz code, it's not actually used.
   */
  public ExpressionInfo createExpressionInfo(String expression, String namespace) {
    ExpressionInfo info;
    synchronized (expressionInfoCache) {
      info = (ExpressionInfo) expressionInfoCache.get(expression);
      if (info == null) {
        info = new ExpressionInfo(expression, namespace);
        expressionInfoCache.put(expression, info);
      }
    }
    return info;
  }

  /**
   * Converts an array of raw expression to an array of within(&lt;expression&gt;) expressions
   */
  public static String[] expressionPatterns2WithinExpressions(String[] expressions) {
    String[] rv = null;
    if (expressions == null) {
      rv = new String[0];
    } else {
      rv = new String[expressions.length];
      for (int i=0; i<expressions.length; i++) {
        rv[i] = expressionPattern2WithinExpression(expressions[i]);
      }
    }
    return rv;
  }

  /**
   * Converts a raw expression to a within(&lt;expression&gt;) expression
   */
  public static String expressionPattern2WithinExpression(String expression) {
    return "within(" + expression + ")";
  }

  public static String expressionPattern2ExecutionExpression(String expression) {
    return "execution(" + expression + ")";
  }

  /**
   * Creates a within expression context for testing to see if a class is within a certain
   * class expression.
   */
  public ExpressionContext createWithinExpressionContext(ClassInfo classInfo) {
    // TODO: Cache me
    ExpressionContext ctxt = new ExpressionContext(PointcutType.WITHIN, classInfo, classInfo);
    return ctxt;
  }

  /**
   * Creates an execution expression context for testing to see of a method matches the execution of a 
   * certain method expression
   */
  public ExpressionContext createExecutionExpressionContext(MemberInfo methodInfo) {
    // TODO: Cache me
    ExpressionContext ctxt = new ExpressionContext(PointcutType.EXECUTION, methodInfo, methodInfo);
    return ctxt;
  }
}