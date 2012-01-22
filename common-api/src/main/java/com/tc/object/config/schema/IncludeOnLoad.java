/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.util.Assert;

/**
 * Represents the on-load xs:choice element
 */
public class IncludeOnLoad {

  private static final int UNDEFINED        = -1;

  public static final int  METHOD           = 0; // String
  public static final int  CALL_CONSTRUCTOR = 1; // Boolean
  public static final int  EXECUTE          = 2; // String

  private int              type;
  private Object           value;

  public IncludeOnLoad() {
    this(UNDEFINED, null);
  }

  public IncludeOnLoad(int type, Object value) {
    this.type = type;
    this.value = value;
  }

  public boolean isCallConstructorOnLoadType() {
    return type == CALL_CONSTRUCTOR;
  }

  public boolean isExecuteScriptOnLoadType() {
    return type == EXECUTE;
  }

  public boolean isCallMethodOnLoadType() {
    return type == METHOD;
  }

  public boolean isCallConstructorOnLoad() {
    if (!isCallConstructorOnLoadType()) { return false; }
    return ((Boolean) value).booleanValue();
  }

  public String getExecuteScript() {
    Assert.eval(isExecuteScriptOnLoadType());
    return (String) value;
  }

  public String getMethod() {
    Assert.eval(isCallMethodOnLoadType());
    return (String) value;
  }

  public void setToCallConstructorOnLoad(boolean b) {
    this.type = CALL_CONSTRUCTOR;
    this.value = Boolean.valueOf(b);
  }

  public void setExecuteScriptOnLoad(String script) {
    this.type = EXECUTE;
    this.value = script;
  }

  public void setMethodCallOnLoad(String method) {
    this.type = METHOD;
    this.value = method;
  }

  public int type() {
    return type;
  }

  public Object value() {
    return value;
  }

  @Override
  public String toString() {
    return "type: " + type + " value=" + value;
  }

}
