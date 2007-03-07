/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.lang;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

/**
 * The purpose of this class to execute a startup action (ie. "start the server", or "start the client", etc) in a
 * thread in the specified thread group. The side effect of doing this is that any more threads spawned by the startup
 * action will inherit the given thread group. It is somewhat fragile, and sometimes impossible (see java.util.Timer) to
 * be explicit about the thread group when spawning threads
 */
public class StartupHelper {

  private final StartupAction action;
  private final String        name;
  private final TCThreadGroup threadGroup;

  public StartupHelper(TCThreadGroup threadGroup, StartupAction action, String name) {
    this.threadGroup = threadGroup;
    this.name = name;
    this.action = action;
  }

  public void startUp() throws Throwable {
    final SynchronizedRef error = new SynchronizedRef(null);

    Runnable r = new Runnable() {
      public void run() {
        try {
          action.execute();
        } catch (Throwable t) {
          error.set(t);
        }
      }
    };

    Thread th = new Thread(threadGroup, r, name);
    th.start();
    th.join();

    Throwable t = (Throwable) error.get();
    if (t != null) { throw t; }
  }

  public interface StartupAction {
    void execute() throws Throwable;
  }

}
