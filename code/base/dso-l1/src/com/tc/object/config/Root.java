/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;


public class Root {
  private final static byte DSO_FINAL_NOT_SET = 0x01;
  private final static byte NOT_DSO_FINAL     = 0x02;
  private final static byte DSO_FINAL         = 0x03;

  private final String      className;
  private final String      fieldName;
  private final String      rootName;
  private final byte        dsoFinal;

  public Root(String className, String fieldName, String rootName) {
    this.className = className;
    this.fieldName = fieldName;
    this.rootName = rootName;
    this.dsoFinal = DSO_FINAL_NOT_SET;
  }

  public Root(String className, String fieldName, String rootName, boolean dsoFinal) {
    this.className = className;
    this.fieldName = fieldName;
    this.rootName = rootName;
    this.dsoFinal = dsoFinal? DSO_FINAL : NOT_DSO_FINAL;
  }

  public boolean matches(String matchClassName, String matchFieldName) {
    return className.equals(matchClassName) && fieldName.equals(matchFieldName);
  }

  public String getClassName() {
    return this.className;
  }

  public String getFieldName() {
    return this.fieldName;
  }

  public String getRootName() {
    return this.rootName == null ? className + "." + fieldName : rootName;
  }
  
  public boolean isDsoFinal(boolean isPrimitive) {
    if (dsoFinal != DSO_FINAL_NOT_SET) {
      return (dsoFinal == DSO_FINAL);
    } else {
      return !isPrimitive;
    }
  }

  private boolean isDsoFinal() {
    return (dsoFinal == DSO_FINAL);
  }

  public String toString() {
    return getClass().getName() + "[className=" + getClassName() + ", fieldName=" + getFieldName() + ", rootName="
           + getRootName() + ", dsoFinal=" + isDsoFinal() + "]";
  }
}