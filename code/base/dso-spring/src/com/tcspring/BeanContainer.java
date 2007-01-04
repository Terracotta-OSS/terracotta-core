/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

/**
 * Container used to store distributed beans
 * 
 * @author Eugene Kuleshov 
 */
public class BeanContainer {
  private Object bean;
  private transient boolean isInitialized;
  private transient ScopedBeanDestructionCallBack destructionCallBack;
  
  public BeanContainer(Object bean, boolean isInitialized) {
    this.bean = bean;
    this.isInitialized = isInitialized;
  }

  public void setBean(Object bean) {
    this.bean = bean;
  }
  
  public Object getBean() {
    return bean;
  }
  
  public void setInitialized(boolean isInitialized) {
    this.isInitialized = isInitialized;
  }
  
  public boolean isInitialized() {
    return isInitialized;
  }
  
  public void setDestructionCallBack(ScopedBeanDestructionCallBack destructionCallBack) {
    this.destructionCallBack = destructionCallBack;
  }
  
  public ScopedBeanDestructionCallBack getDestructionCallBack() {
    return destructionCallBack;
  }
  
  public String toString() {
    return "isInitialized:" + isInitialized + " bean:"+bean+" destructionCallback:"+destructionCallBack;
  }
  
}
