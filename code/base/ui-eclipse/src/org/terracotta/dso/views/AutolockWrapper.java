/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.views;

import com.terracottatech.config.LockLevel;

public class AutolockWrapper extends LockWrapper {
  AutolockWrapper(LocksWrapper parent, int index) {
    super(parent, index);
  }
  
  String getMethodExpression() {
    return m_parent.getAutolockArray(m_index).getMethodExpression();
  }
  
  void setMethodExpression(String methodExpr) {
    m_parent.getAutolockArray(m_index).setMethodExpression(methodExpr);
  }
  
  LockLevel getLevel() {
    return m_parent.getAutolockArray(m_index).xgetLockLevel();
  }
  
  void setLevel(LockLevel.Enum level) {
    m_parent.getAutolockArray(m_index).setLockLevel(level);
  }
  
  boolean getAutoSynchronized() {
    return m_parent.getAutolockArray(m_index).getAutoSynchronized();
  }

  void setAutoSynchronized(boolean autoSynchronized) {
    m_parent.getAutolockArray(m_index).setAutoSynchronized(autoSynchronized);
  }
  
  void unsetAutoSynchronized() {
    m_parent.getAutolockArray(m_index).unsetAutoSynchronized();
  }
  
  void remove() {
    m_parent.removeAutolock(m_index);
  }
}
