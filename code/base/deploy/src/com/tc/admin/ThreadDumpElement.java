/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;


import com.tc.admin.common.ApplicationContext;

import java.util.concurrent.Future;

import javax.swing.SwingUtilities;

public class ThreadDumpElement extends ThreadDumpTreeNode {
  private ThreadDumpEntry threadDumpEntry;

  ThreadDumpElement(ApplicationContext appContext, String name, Future<String> threadDumpFuture) {
    super(appContext);
    setUserObject(name + " " + appContext.format("waiting"));
    setName(name);
    threadDumpEntry = new ThreadDumpEntry(appContext, threadDumpFuture) {
      public void run() {
        super.run();
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            setUserObject(getName());
            nodeChanged();
          }
        });
      }
    };
  }

  String getThreadDump() {
    return threadDumpEntry.getThreadDump();
  }

  public boolean isDone() {
    return threadDumpEntry.isDone();
  }

  public void cancel() {
    if (!isDone()) {
      threadDumpEntry.cancel();
      setUserObject(getName() + " " + appContext.format("canceled"));
      nodeChanged();
    }
  }

  String getContent() {
    return threadDumpEntry.getContent();
  }

  String getSource() {
    return toString();
  }
}
