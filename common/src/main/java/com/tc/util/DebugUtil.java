/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
