/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import java.util.Date;

public class ClusterThreadDumpEntry extends ThreadDumpTreeNode {
  private String m_content;

  ClusterThreadDumpEntry() {
    super(new Date());
  }

  void add(String clientAddr, String threadDump) {
    add(new ThreadDumpElement(clientAddr, threadDump));
  }

  Date getTime() {
    return (Date) getUserObject();
  }

  String getContent() {
    if (m_content != null) return m_content;

    StringBuffer sb = new StringBuffer();
    String nl = System.getProperty("line.separator");
    for (int i = 0; i < getChildCount(); i++) {
      ThreadDumpElement tde = (ThreadDumpElement) getChildAt(i);
      sb.append("---------- ");
      sb.append(tde.getSource());
      sb.append(" ----------");
      sb.append(nl);
      sb.append(nl);

      sb.append(tde.getContent());
    }
    return m_content = sb.toString();
  }
}
