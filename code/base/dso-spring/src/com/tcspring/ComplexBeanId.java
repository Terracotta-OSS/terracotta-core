/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

public class ComplexBeanId {
  private transient boolean isVirtualized = false;
  
  private transient ComplexBeanId equalPeer = null;
  
  private String scopeId = null;
  private String beanName = null;
  
  public ComplexBeanId(String newScopeId, String newBeanName, boolean virtualized) {
    this.scopeId = newScopeId;
    this.beanName = newBeanName;
    this.isVirtualized = virtualized;
  }
  
  public boolean equals(Object obj) {
    boolean rtv = obj != null && obj.getClass().equals(this.getClass());
    if (rtv) {
      ComplexBeanId beanId = (ComplexBeanId) obj;
      rtv = isEqual(beanId.scopeId, this.scopeId) && isEqual(beanId.beanName, this.beanName);
      if (rtv) {
        this.equalPeer = (ComplexBeanId) obj;
        ((ComplexBeanId) obj).equalPeer = this;
      }
    }

    return rtv;
  }
  
  public int hashCode() {
    return (scopeId==null? 0 : scopeId.hashCode()) + beanName.hashCode();
  }
  
  public String toString() {
    return "" + scopeId + "--" + beanName;
  }

  public String getBeanName() {
    return beanName;
  }

  public Object getScopeId() {
    return scopeId;
  }
  
  private boolean isEqual(Object obj1, Object obj2) {
    return obj1==null ? obj2==null : obj1.equals(obj2);
  }

  public ComplexBeanId getEqualPeer() {
    return equalPeer;
  }

  public void setEqualPeer(ComplexBeanId equalPeer) {
    this.equalPeer = equalPeer;
  }

  public boolean isVirtualized() {
    return isVirtualized;
  }

  public void setVirtualized(boolean isVirtualized) {
    this.isVirtualized = isVirtualized;
  }

}
