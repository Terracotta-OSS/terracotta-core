/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.lang;

import com.tc.logging.NullTCLogger;

import junit.framework.TestCase;

public class StartupHelperTest extends TestCase {

  final TCThreadGroup group = new TCThreadGroup(new ThrowableHandler(new NullTCLogger()));

  public void testException() throws Throwable {
    StartupHelper helper = new StartupHelper(group, new StartupHelper.StartupAction() {
      public void execute() throws Throwable {
        throw new RuntimeException("da bomb");
      }
    }, "name");

    try {
      helper.startUp();
      fail();
    } catch (RuntimeException re) {
      assertEquals("da bomb", re.getMessage());
    }
  }

  public void testGroup() throws Throwable {
    StartupHelper helper = new StartupHelper(group, new StartupHelper.StartupAction() {
      public void execute() throws Throwable {
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        if (tg != group) { throw new AssertionError("wrong thread group: " + tg); }
      }
    }, "name");

    helper.startUp();
  }

  public void testName() throws Throwable {
    StartupHelper helper = new StartupHelper(group, new StartupHelper.StartupAction() {
      public void execute() throws Throwable {
        String name = Thread.currentThread().getName();
        if (!"bobcat".equals(name)) { throw new AssertionError("wrong thread name: " + name); }
      }
    }, "bobcat");

    helper.startUp();
  }

}
