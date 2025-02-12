/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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
