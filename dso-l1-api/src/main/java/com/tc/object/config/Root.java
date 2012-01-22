/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config;

import com.tc.aspectwerkz.expression.ExpressionContext;
import com.tc.aspectwerkz.expression.ExpressionVisitor;
import com.tc.aspectwerkz.expression.PointcutType;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.object.bytecode.aspectwerkz.ExpressionHelper;

public class Root {

  private final String   className;
  private final String   fieldNameOrExpression;
  private final String   rootName;
  private final DsoFinal dsoFinal;
  private final Type     type;

  public Root(String rootExpr) {
    this(null, rootExpr, null, DsoFinal.NOT_SET, Type.FIELD_EXPR);
  }

  public Root(String rootExpr, String rootName) {
    this(null, rootExpr, rootName, DsoFinal.NOT_SET, Type.FIELD_EXPR);
  }

  public Root(String className, String fieldName, String rootName) {
    this(className, fieldName, rootName, DsoFinal.NOT_SET, Type.FIELD_NAME);
  }

  public Root(String className, String fieldName, String rootName, boolean dsoFinal) {
    this(className, fieldName, rootName, (dsoFinal ? DsoFinal.TRUE : DsoFinal.FALSE), Type.FIELD_NAME);
  }

  private Root(String className, String fieldName, String rootName, DsoFinal dsoFinal, Type type) {
    this.className = className;
    this.fieldNameOrExpression = fieldName;
    this.rootName = rootName;
    this.dsoFinal = dsoFinal;
    this.type = type;
  }

  public boolean isExpression() {
    return (type == Type.FIELD_EXPR);
  }
  public String getClassName() {
    if (type != Type.FIELD_NAME) { throw new IllegalStateException(); }
    return this.className;
  }

  public String getFieldName() {
    if (type != Type.FIELD_NAME) { throw new IllegalStateException(); }
    return this.fieldNameOrExpression;
  }

  public String getFieldExpression() {
    if (type != Type.FIELD_EXPR) { throw new IllegalStateException(); }
    return this.fieldNameOrExpression;
  }
  
  public String getRootName(FieldInfo fieldInfo) {
    return rootName == null ? fieldInfo.getDeclaringType().getName() + "." + fieldInfo.getName() : rootName;
  }

  public boolean isDsoFinal(boolean isPrimitive) {
    if (dsoFinal != DsoFinal.NOT_SET) {
      return (dsoFinal == DsoFinal.TRUE);
    } else {
      return !isPrimitive;
    }
  }

  private boolean isDsoFinal() {
    return (dsoFinal == DsoFinal.TRUE);
  }

  public boolean matches(ClassInfo ci, ExpressionHelper expressionHelper) {
    if (type == Type.FIELD_EXPR) {
      ExpressionContext ctxt = expressionHelper.createWithinExpressionContext(ci);
      ExpressionVisitor visitor = expressionHelper.createExpressionVisitor(fieldNameOrExpression);
      return visitor.match(ctxt);
    } else if (type == Type.FIELD_NAME) {
      return ci.getName().equals(className);
    }
    throw new AssertionError();
  }
  
  public boolean matches(FieldInfo fi, ExpressionHelper expressionHelper) {
    if (type == Type.FIELD_EXPR) {
      ExpressionVisitor visitor = expressionHelper.createExpressionVisitor("get(" + fieldNameOrExpression + ")");
      return visitor.match(new ExpressionContext(PointcutType.GET, fi, fi));
    } else if (type == Type.FIELD_NAME) {
      //
      return fi.getDeclaringType().getName().equals(className) && fi.getName().equals(fieldNameOrExpression);
    }

    throw new AssertionError();
  }

  public String toString() {
    return getClass().getName() + "[className=" + className + ", fieldNameOrExpression=" + fieldNameOrExpression + ", rootName="
           + rootName + ", dsoFinal=" + isDsoFinal() + "]";
  }

  private static class Type {
    static final Type FIELD_NAME = new Type();
    static final Type FIELD_EXPR = new Type();
  }

  private static class DsoFinal {
    private final String s;

    public DsoFinal(String s) {
      this.s = s;
    }

    public String toString() {
      return s;
    }

    static final DsoFinal NOT_SET = new DsoFinal("not set");
    static final DsoFinal TRUE    = new DsoFinal("true");
    static final DsoFinal FALSE   = new DsoFinal("false");
  }

}
