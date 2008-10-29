/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import java.util.concurrent.Future;

import javax.swing.SwingUtilities;

public class ThreadDumpElement extends ThreadDumpTreeNode {
  private String          m_clientAddr;
  private ThreadDumpEntry m_threadDumpEntry;

  ThreadDumpElement(String clientAddr, Future<String> threadDumpFuture) {
    super(clientAddr + " " + AdminClient.getContext().format("waiting"));
    m_clientAddr = clientAddr;
    m_threadDumpEntry = new ThreadDumpEntry(threadDumpFuture) {
      public void run() {
        super.run();
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            setUserObject(m_clientAddr);
            nodeChanged();
          }
        });
      }
    };
  }

  String getThreadDump() {
    return m_threadDumpEntry.getThreadDump();
  }

  public boolean isDone() {
    return m_threadDumpEntry.isDone();
  }

  public void cancel() {
    if (!isDone()) {
      m_threadDumpEntry.cancel();
      setUserObject(m_clientAddr + " "+ AdminClient.getContext().format("canceled"));
      nodeChanged();
    }
  }

  String getContent() {
    return m_threadDumpEntry.getContent();
  }

  String getSource() {
    return toString();
  }
}
