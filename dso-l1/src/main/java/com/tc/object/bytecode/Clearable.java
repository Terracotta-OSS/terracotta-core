/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

public interface Clearable {

  public int __tc_clearReferences(int toClear);
  
  public void setEvictionEnabled(boolean enabled);

  public boolean isEvictionEnabled();
}
