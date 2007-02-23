/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.views;


public class NamedLocksWrapper {
  private LocksWrapper m_locks;
  
  NamedLocksWrapper(LocksWrapper locks) {
    m_locks = locks;
  }
  
  LockWrapper[] createNamedLockWrappers() {
    int count = m_locks.sizeOfNamedLockArray();
    LockWrapper[] result = new LockWrapper[count];
    
    for(int i = 0; i < count; i++) {
      result[i] = new NamedLockWrapper(m_locks, i);
    }
    
    return result;
  }
  
  int sizeOfNamedLockArray() {
    return m_locks.sizeOfNamedLockArray();
  }
  
  public String toString() {
    return "Named locks";
  }
}
