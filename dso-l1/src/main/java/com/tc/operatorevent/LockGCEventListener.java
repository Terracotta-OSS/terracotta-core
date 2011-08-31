/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

public interface LockGCEventListener {
  void fireLockGCEvent(int gcCount);
}
