/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

/**
 * This a temporary class to hold a debug flag and should be removed after the CyclicBarrier bug is found. TODO: Remove
 * after the bug is found.
 */
public class DebugUtil {
  public static boolean DEBUG  = false;

  private static String nodeId = String.valueOf(System.identityHashCode(DebugUtil.class));

  public static void setNodeId(String v) {
    nodeId = v + ".[" + String.valueOf(System.identityHashCode(DebugUtil.class)) + "]";
  }

  public static String nodeId() {
    return nodeId;
  }

  public static void trace(String msg) {
    final String m = "\n\n@@@@ " + nodeId() + " -> " + msg + "\n\n";
    System.err.println(m);
  }

  public static void trace(String method, String msg) {
    final String m = "\n\n@@@@ " + nodeId() + " -> " + method + " -> " + msg + "\n\n";
    System.err.println(m);
  }
}
