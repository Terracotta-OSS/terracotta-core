/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session.util;

public class Assert {

  public static void pre(boolean v) {
    if (!v) throw new AssertionError("Precondition Failed");
  }

  public static void post(boolean v) {
    if (!v) throw new AssertionError("Postcondition Failed");
  }

  public static void inv(boolean v) {
    if (!v) throw new AssertionError("Invariant Failed");
  }

}
