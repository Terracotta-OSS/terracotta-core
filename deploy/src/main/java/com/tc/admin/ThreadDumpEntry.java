/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;


import com.tc.admin.common.ApplicationContext;

import java.awt.Point;
import java.util.Date;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ThreadDumpEntry implements Runnable {
  protected ApplicationContext appContext;
  private Future<String>       future;
  private String               text;
  private Date                 time;
  private Point                viewPosition;

  private static final int     DEFAULT_TIMEOUT_SECONDS = 300;
  private static final int     TIMEOUT_SECONDS         = Integer.getInteger("com.tc.admin.ThreadDumpEntry.timeout",
                                                                            DEFAULT_TIMEOUT_SECONDS);

  public ThreadDumpEntry(ApplicationContext appContext, Future<String> threadDumpFuture) {
    this.appContext = appContext;
    this.future = threadDumpFuture;
    time = new Date();
    appContext.submit(this);
  }

  public void run() {
    String result;
    try {
      result = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      future.cancel(true);
      result = appContext.format("thread.dump.timeout.msg", TIMEOUT_SECONDS);
    } catch (CancellationException ce) {
      result = appContext.format("canceled");
    } catch (Exception e) {
      result = e.getMessage();
    }
    setThreadDump(result);
  }

  synchronized void setThreadDump(String text) {
    this.text = text;
  }

  public synchronized String getThreadDump() {
    return text;
  }

  public boolean isDone() {
    return getThreadDump() != null;
  }

  public void cancel() {
    if (!isDone()) {
      if (!future.cancel(true)) {
        setThreadDump("Failed to cancel!");
      }
    }
  }

  String getContent() {
    if (isDone()) {
      return getThreadDump();
    } else {
      return appContext.format("waiting");
    }
  }

  public Date getTime() {
    return new Date(time.getTime());
  }

  public void setViewPosition(Point pos) {
    viewPosition = pos;
  }

  public Point getViewPosition() {
    return viewPosition;
  }

  public String toString() {
    return time.toString();
  }
}