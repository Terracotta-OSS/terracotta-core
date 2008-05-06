/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import java.io.Serializable;

public class WithoutUID implements Serializable {
  // NOTE: If you use a different compiler (other than javac), this number might be different
  public static final long EXPECTED_UID = 5565025854900417463L;

  // This "timmy" stuff is just to make this class a little\
  // more interesting (ie. add some fields, method and what not)
  private int              timmy        = 4;

  public WithoutUID() {
    // nada
  }

  public int getTimmy() {
    return timmy;
  }

}
