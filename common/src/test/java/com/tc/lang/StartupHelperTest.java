/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.lang;


import com.tc.logging.NullTCLogger;

import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;

public class StartupHelperTest extends TestCase {

  public void testException() throws Throwable {
    final AtomicReference<Throwable> error = new AtomicReference(null);

    ThreadGroup group = new ThreadGroup("group") {
      @Override
      public void uncaughtException(Thread t, Throwable e) {
        error.set(e);
      }
    };

    final RuntimeException re = new RuntimeException("da bomb");

    StartupHelper helper = new StartupHelper(group, new StartupHelper.StartupAction() {
      @Override
      public void execute() throws Throwable {
        throw re;
      }
    });

    try {
      helper.startUp();
    } catch (RuntimeException e) {
      //
    }

    RuntimeException thrown = (RuntimeException) error.get();
    if (thrown == null) {
      fail("no exception delivered to group");
    }

    assertTrue(thrown == re);
  }

  public void testGroup() throws Throwable {
    final TCThreadGroup group = new TCThreadGroup(new ThrowableHandler(new NullTCLogger()));

    StartupHelper helper = new StartupHelper(group, new StartupHelper.StartupAction() {
      @Override
      public void execute() throws Throwable {
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        if (tg != group) { throw new AssertionError("wrong thread group: " + tg); }
      }
    });

    helper.startUp();
  }

}
