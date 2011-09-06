/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.restart;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TestThreadGroup extends ThreadGroup {

  public TestThreadGroup(ThreadGroup parent, String name) {
    super(parent, name);
  }

  private final Set throwables = Collections.synchronizedSet(new HashSet());

  public void uncaughtException(Thread thread, Throwable throwable) {
    super.uncaughtException(thread, throwable);
    throwables.add(throwable);
  }
  
  public Collection getErrors() {
    return new HashSet(throwables);
  }
}