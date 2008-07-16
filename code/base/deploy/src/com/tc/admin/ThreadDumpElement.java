/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

public class ThreadDumpElement extends ThreadDumpTreeNode {
  private String m_threadDump;

  ThreadDumpElement(String clientAddr, String threadDump) {
    super(clientAddr);
    m_threadDump = threadDump;
  }

  String getThreadDump() {
    return m_threadDump;
  }

  String getContent() {
    return getThreadDump();
  }

  String getSource() {
    return toString();
  }
}
