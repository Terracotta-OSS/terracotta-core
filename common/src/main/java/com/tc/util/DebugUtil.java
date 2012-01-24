/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

/**
 * This class holds a debug flag and is used to debug certain kinds of problem in our test environment.
 */
public class DebugUtil {
  public static boolean DEBUG  = false;

  private static String nodeId = String.valueOf(System.identityHashCode(DebugUtil.class));

  public static void setNodeId(String v) {
    nodeId = v + ".[" + String.valueOf(System.identityHashCode(DebugUtil.class)) + "]";
  }

  private static String nodeId() {
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
