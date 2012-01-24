/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode.hook.impl;


/**
 * Helper utility methods
 */
public class Util {

  private Util() {
    //
  }

  /**
   * System.exit() without an exception
   */
  public static void exit() {
    exit(null);
  }

  /**
   * Dump an exception and System.exit().
   * 
   * @param t Exception
   */
  public static void exit(Throwable t) {
    if (t != null) {
      t.printStackTrace(System.err);
      System.err.flush();
    }

    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      // ignore
    }

    System.exit(1);
  }

}
