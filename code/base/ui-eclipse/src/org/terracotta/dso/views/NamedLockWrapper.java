/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.views;

import com.terracottatech.config.LockLevel;

public class NamedLockWrapper extends LockWrapper {
  NamedLockWrapper(LocksWrapper parent, int index) {
    super(parent, index);
  }
  
  String getMethodExpression() {
    return m_parent.getNamedLockArray(m_index).getMethodExpression();
  }

  void setMethodExpression(String methodExpr) {
    m_parent.getNamedLockArray(m_index).setMethodExpression(methodExpr);
  }
  
  LockLevel getLevel() {
    return m_parent.getNamedLockArray(m_index).xgetLockLevel();
  }
  
  void setLevel(LockLevel.Enum level) {
    m_parent.getNamedLockArray(m_index).setLockLevel(level);
  }
  
  protected void remove() {
    m_parent.removeNamedLock(m_index);
  }
}
