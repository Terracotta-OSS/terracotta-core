/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
