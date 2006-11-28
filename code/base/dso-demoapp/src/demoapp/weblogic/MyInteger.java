/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package demoapp.weblogic;

public class MyInteger {

  private final int i;

  public MyInteger(int i) {
    this.i = i;
  }

  public String toString() {
    return getClass().getName() + "(" + i + ")";
  }

}
