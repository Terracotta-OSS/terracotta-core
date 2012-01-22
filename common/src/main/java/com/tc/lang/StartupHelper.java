/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.lang;

import com.tc.util.runtime.Vm;

import java.lang.reflect.Field;

/**
 * The purpose of this class to execute a startup action (ie. "start the server", or "start the client", etc) in the
 * specified thread group. The side effect of doing this is that any more threads spawned by the startup action will
 * inherit the given thread group. It is somewhat fragile, and sometimes impossible (see java.util.Timer) to be explicit
 * about the thread group when spawning threads <br>
 * <br>
 * XXX: At the moment, this class uses a hack of adjusting the current thread's group to the desired target group. A
 * nicer approach would be to start a new thread in the desiered target group and run the action in that thread and
 * join() it, except that can introduce locking problems (see MNK-65)
 */
public class StartupHelper {

  private final StartupAction action;
  private final ThreadGroup   targetThreadGroup;

  public StartupHelper(ThreadGroup threadGroup, StartupAction action) {
    this.targetThreadGroup = threadGroup;
    this.action = action;
  }

  public void startUp() {
    Thread currentThread = Thread.currentThread();
    ThreadGroup origThreadGroup = currentThread.getThreadGroup();

    setThreadGroup(currentThread, targetThreadGroup);

    Throwable actionError = null;
    try {
      action.execute();
    } catch (Throwable t) {
      actionError = t;
    } finally {
      setThreadGroup(currentThread, origThreadGroup);
    }

    if (actionError != null) {
      targetThreadGroup.uncaughtException(currentThread, actionError);
    }
  }

  public interface StartupAction {
    void execute() throws Throwable;
  }

  private static void setThreadGroup(Thread thread, ThreadGroup group) {
    String fieldName = Vm.isIBM() ? "threadGroup" : "group";
    Class c = Thread.class;

    try {
      Field groupField = c.getDeclaredField(fieldName);
      groupField.setAccessible(true);
      groupField.set(thread, group);
    } catch (Exception e) {
      if (e instanceof RuntimeException) { throw (RuntimeException) e; }
      throw new RuntimeException(e);
    }
  }
}
