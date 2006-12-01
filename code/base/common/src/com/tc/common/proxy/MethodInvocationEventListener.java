/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.common.proxy;

/**
 * Listner interface for those are interested in method invocation events
 */
public interface MethodInvocationEventListener {
  public void methodInvoked(MethodInvocationEvent event);
}
