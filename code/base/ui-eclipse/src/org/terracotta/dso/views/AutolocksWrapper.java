/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.views;


public class AutolocksWrapper {
  private LocksWrapper m_locks;
  private AutolockWrapper[] children;
  
  AutolocksWrapper(LocksWrapper locks) {
    m_locks = locks;
  }
  
  AutolockWrapper[] createAutolockWrappers() {
    int count = m_locks.sizeOfAutolockArray();

    children = new AutolockWrapper[count];
    for(int i = 0; i < count; i++) {
      children[i] = new AutolockWrapper(m_locks, i);
    }
    
    return children;
  }
  
  AutolockWrapper[] getChildren() {
    return children;
  }
  
  AutolockWrapper getChildAt(int index) {
    return children != null ? children[index] : null;
  }
  
  int sizeOfAutolockArray() {
    return m_locks.sizeOfAutolockArray();
  }

  public String toString() {
    return "Autolocks";
  }
}
