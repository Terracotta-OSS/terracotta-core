/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.views;

import com.terracottatech.config.LockLevel;

public abstract class LockWrapper {
  protected LocksWrapper m_parent;
  protected int m_index;

  public LockWrapper(LocksWrapper parent, int index) {
    m_parent = parent;
    m_index = index;
  }

  LocksWrapper getParent() {
    return m_parent;
  }

  int getIndex() {
    return m_index;
  }
  
  abstract String getMethodExpression();
  abstract void setMethodExpression(String methodExpr);
  abstract void remove();
  abstract LockLevel getLevel();
  abstract void setLevel(LockLevel.Enum enumValue);
  
  public String toString() {
    return getMethodExpression();
  }
}