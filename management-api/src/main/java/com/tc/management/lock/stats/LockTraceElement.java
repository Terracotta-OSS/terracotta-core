/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management.lock.stats;

import java.util.Collection;

public interface LockTraceElement {
  String getConfigElement();
  
  StackTraceElement getStackFrame();
  
  LockStats getStats();
  
  boolean hasChildren();
  
  Collection children();
}
