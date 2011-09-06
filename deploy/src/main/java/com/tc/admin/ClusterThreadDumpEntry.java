/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;


import com.tc.admin.common.ApplicationContext;

import java.util.Date;
import java.util.concurrent.Future;

public class ClusterThreadDumpEntry extends ThreadDumpTreeNode {
  private String text;

  ClusterThreadDumpEntry(ApplicationContext appContext) {
    super(appContext, new Date());
  }

  void add(String clientAddr, Future<String> threadDump) {
    add(new ThreadDumpElement(appContext, clientAddr, threadDump));
  }

  Date getTime() {
    return (Date) getUserObject();
  }

  boolean isDone() {
    for (int i = 0; i < getChildCount(); i++) {
      ThreadDumpElement tde = (ThreadDumpElement) getChildAt(i);
      if (!tde.isDone()) return false;
    }
    return true;
  }

  void cancel() {
    for (int i = 0; i < getChildCount(); i++) {
      ThreadDumpElement tde = (ThreadDumpElement) getChildAt(i);
      if (!tde.isDone()) {
        tde.cancel();
      }
    }
  }

  String getContent() {
    if (text != null) return text;
    String result;
    boolean isDone = isDone();
    if (isDone) {
      StringBuffer sb = new StringBuffer();
      String nl = System.getProperty("line.separator");
      for (int i = 0; i < getChildCount(); i++) {
        ThreadDumpElement tde = (ThreadDumpElement) getChildAt(i);
        sb.append("---------- ");
        sb.append(tde.getSource());
        sb.append(" ----------");
        sb.append(nl);
        sb.append(nl);
        if (tde.isDone()) {
          sb.append(tde.getContent());
          sb.append(nl);
        }
      }
      result = sb.toString();
    } else {
      result = appContext.format("waiting");
    }
    if (isDone) {
      text = result;
    }
    return result;
  }
}
