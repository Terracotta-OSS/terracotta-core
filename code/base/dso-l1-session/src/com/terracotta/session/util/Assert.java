/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
