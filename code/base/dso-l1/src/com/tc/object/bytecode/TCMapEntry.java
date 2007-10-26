/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import java.util.Map.Entry;

public interface TCMapEntry extends Entry {
  public final static String TC_ISVALUEFAULTEDIN_METHOD_NAME = ByteCodeUtil.TC_METHOD_PREFIX + "isValueFaultedIn";
  public final static String TC_ISVALUEFAULTEDIN_METHOD_DESC = "()Z";
  
  public boolean __tc_isValueFaultedIn();
}