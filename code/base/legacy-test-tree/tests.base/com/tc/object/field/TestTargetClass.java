/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.field;


public class TestTargetClass {
  private int field = 0;
  
  protected int getField() {
    // this method here just to silence the compiler warning
    return field;
  }

}
