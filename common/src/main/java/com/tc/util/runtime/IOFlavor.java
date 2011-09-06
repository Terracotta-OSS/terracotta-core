/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.runtime;

/**
 * @author teck 
 */
public class IOFlavor {

  private static boolean forceJDK13 = false;

  public static void forceJDK13() {
    forceJDK13 = true;
  }

  public static boolean isNioAvailable() {
    if (forceJDK13) { return false; }

    try {
      Class.forName("java.nio.ByteBuffer");
    } catch (ClassNotFoundException e) {
      return false;
    }

    return true;
  }

}