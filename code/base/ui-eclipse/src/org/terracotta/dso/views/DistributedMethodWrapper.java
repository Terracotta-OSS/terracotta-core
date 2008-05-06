/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.views;

public class DistributedMethodWrapper {
  private DistributedMethodsWrapper m_parent;
  private int m_index;
  
  DistributedMethodWrapper(DistributedMethodsWrapper parent, int index) {
    m_parent = parent;
    m_index = index;
  }
  
  DistributedMethodsWrapper getParent() {
    return m_parent;
  }
  
  String getMethodName() {
    return m_parent.getMethodExpressionArray(m_index);
  }
  
  void remove() {
    m_parent.removeMethodExpression(m_index);
  }
  
  public String toString() {
    return getMethodName();
  }
}
