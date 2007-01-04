/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;


public class ComplexBeanId {
  private String scopeId = null;
  private String beanName = null;
  
  public ComplexBeanId(String beanName) {
    this("singleton", beanName);
  }
  
  public ComplexBeanId(String newScopeId, String newBeanName) {
    this.scopeId = newScopeId;
    this.beanName = newBeanName;
  }

  public String getBeanName() {
    return beanName;
  }
  
  public String getScopeId() {
    return scopeId;
  }
  
  public int hashCode() {
    return (31 * scopeId.hashCode()) + beanName.hashCode();
  }

  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    
    ComplexBeanId other = (ComplexBeanId) obj;
    if (beanName == null) {
      if (other.beanName != null) {
        return false;
      }
    } else if (!beanName.equals(other.beanName)) {
      return false;
    }
    
    if (scopeId == null) {
      if (other.scopeId != null) {
        return false;
      }
    } else if (!scopeId.equals(other.scopeId)) {
      return false;
    }
    
    return true;
  }

  public String toString() {
    return scopeId + ":" + beanName;
  }
  
}

