/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.field;


public class TestTargetClass {
  private int field = 0;
  
  protected int getField() {
    // this method here just to silence the compiler warning
    return field;
  }

}
