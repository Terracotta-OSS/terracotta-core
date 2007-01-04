/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

import org.springframework.beans.factory.BeanNameAware;

import com.tc.aspectwerkz.proxy.Uuid;

public class SimpleBean implements ISimpleBean, BeanNameAware {
  private transient long id                    = System.identityHashCode(this) + Uuid.newUuid();
  
  private transient long timeStamp             = System.currentTimeMillis();
  private long sharedId                        = 0;
  
  private static int instanceCnt               = 0;
  
  private static String staticField            = null;
  private transient String transientField     = null;
  private String field                        = null;
  private String dsoTransientField            = null;
  
  private ISimpleBean sharedRef               = null;
  private transient ISimpleBean transientRef  = null;
  private transient ISimpleBean dsoTransientRef  = null;

  private transient String beanName;
    
  public SimpleBean() {
    synchronized(SimpleBean.class) {
      instanceCnt ++;               // this should have the number of instance in one CL, assuming constructor is invoked
      sharedId = timeStamp;
    }
  }
  
  public int getInstanceCnt() {
    synchronized(SimpleBean.class) {
      return instanceCnt;
    }
  }
  
  synchronized public String getStaticField() {
    return staticField;
  }
  
  synchronized public void setStaticField(String staticField) {
    SimpleBean.staticField = staticField;
  }
  
  synchronized public String getDsoTransientField() {
    return dsoTransientField;
  }
  
  synchronized public void setDsoTransientField(String dsoTransientField) {
    this.dsoTransientField = dsoTransientField;
  }
  
  synchronized public String getField() {
    return field;
  }
  
  synchronized public void setField(String field) {
    this.field = field;
  }
  
  synchronized public ISimpleBean getTransientRef() {
    return transientRef;
  }
  
  synchronized public void setTransientRef(ISimpleBean transientChild) {
    this.transientRef = transientChild;
  }
  
  synchronized public String getTransientField() {
    return transientField;
  }
  
  synchronized public void setTransientField(String transientField) {
    this.transientField = transientField;
  }

  public int getHashCode() {
    return hashCode();
  }
  
  synchronized public long getId() {
    return id;
  }

  synchronized public long getSharedId() {
    return sharedId;
  }

  synchronized public void setSharedId(long sharedId) {
    this.sharedId = sharedId;
  }

  synchronized public ISimpleBean getDsoTransientRef() {
    return dsoTransientRef;
  }

  synchronized public void setDsoTransientRef(ISimpleBean dsoTransientRef) {
    this.dsoTransientRef = dsoTransientRef;
  }

  synchronized public long getSharedRefId() {
    return this.sharedRef == null ? -1 : sharedRef.getId();
  }
    
  synchronized public ISimpleBean getSharedRef() {
    return sharedRef;
  }

  synchronized public void setSharedRef(ISimpleBean sharedRef) {
    this.sharedRef = sharedRef;
  }

  public long getTimeStamp() {
    return timeStamp;
  }

  public static class SBParent extends SimpleBean {
    static private int myInstanceCnt = 0;
    public SBParent() {
      synchronized(SimpleBean.class) {
        myInstanceCnt ++;               // this should have the number of instance of this subtype in one CL, assuming constructor is invoked
      }
    }
   
    public int getInstanceCnt() {
      synchronized(SimpleBean.class) {
        return myInstanceCnt;
      }
    }
  }

  public String getBeanName() {
    return beanName;
  }

  public void setBeanName(String beanName) {
    this.beanName = beanName;
  }  
}
