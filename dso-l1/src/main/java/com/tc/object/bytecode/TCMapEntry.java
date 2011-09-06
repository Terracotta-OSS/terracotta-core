/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

public interface TCMapEntry {
  public final static String TC_RAWSETVALUE_METHOD_NAME = ByteCodeUtil.TC_METHOD_PREFIX + "rawSetValue";
  public final static String TC_RAWSETVALUE_METHOD_DESC = "(Ljava/lang/Object;)V";

  public final static String TC_ISVALUEFAULTEDIN_METHOD_NAME = ByteCodeUtil.TC_METHOD_PREFIX + "isValueFaultedIn";
  public final static String TC_ISVALUEFAULTEDIN_METHOD_DESC = "()Z";
  
  public void __tc_rawSetValue(Object value);
  public boolean __tc_isValueFaultedIn();
}