/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.object.TCObject;

public interface Manageable {
  public static final String CLASS = "com/tc/object/bytecode/Manageable";
  public static final String TYPE  = "L" + CLASS + ";";

  public void __tc_managed(TCObject t);

  public TCObject __tc_managed();
  
  public boolean __tc_isManaged();

}
