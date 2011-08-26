/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.views;


public class NamedLocksWrapper {
  private LocksWrapper m_locks;
  private NamedLockWrapper[] children;
  
  NamedLocksWrapper(LocksWrapper locks) {
    m_locks = locks;
  }
  
  NamedLockWrapper[] createNamedLockWrappers() {
    int count = m_locks.sizeOfNamedLockArray();
    children = new NamedLockWrapper[count];
    
    for(int i = 0; i < count; i++) {
      children[i] = new NamedLockWrapper(m_locks, i);
    }
    
    return children;
  }

  NamedLockWrapper[] getChildren() {
    return children;
  }
  
  NamedLockWrapper getChildAt(int index) {
    return children != null ? children[index] : null;
  }
  
  int sizeOfNamedLockArray() {
    return m_locks.sizeOfNamedLockArray();
  }
  
  public String toString() {
    return "Named locks";
  }
}
