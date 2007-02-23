/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.views;


public class AutolocksWrapper {
  private LocksWrapper m_locks;
  
  AutolocksWrapper(LocksWrapper locks) {
    m_locks = locks;
  }
  
  AutolockWrapper[] createAutolockWrappers() {
    int count = m_locks.sizeOfAutolockArray();
    AutolockWrapper[] result = new AutolockWrapper[count];
    
    for(int i = 0; i < count; i++) {
      result[i] = new AutolockWrapper(m_locks, i);
    }
    
    return result;
  }
  
  int sizeOfAutolockArray() {
    return m_locks.sizeOfAutolockArray();
  }

  public String toString() {
    return "Autolocks";
  }
}
