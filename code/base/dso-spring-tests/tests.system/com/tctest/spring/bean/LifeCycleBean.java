/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.support.AbstractApplicationContext;

import com.tc.aspectwerkz.proxy.Uuid;

import java.util.ArrayList;
import java.util.List;

public class LifeCycleBean implements InitializingBean, ApplicationContextAware,
                                      BeanNameAware, DisposableBean, ILifeCycle {
  private transient long mSystemId;
  
  private List mProp; 
  
  private List mInvocationRecords = new ArrayList();
  private transient ApplicationContext mAppCtx = null;
  private transient String mBeanName = null;

  public LifeCycleBean() {
    mSystemId = System.identityHashCode(this) + Uuid.newUuid();
  }
  
  public long getSystemId() { return mSystemId; }
  
  synchronized public List getInvocationRecords() {
    return mInvocationRecords;
  }
  
  /**
   * Is this method within a synchronization block ????
   */
  public void afterPropertiesSet() {
    // this property will be initialized and shared
    mProp.add("" + mSystemId);
    this.mInvocationRecords.add("afterPropertiesSet-" + mSystemId);
    System.err.println("afterPropertiesSet-" + mSystemId);
  }

  /**
   * Is this method within a synchronization block ????
   */ 
  public void setApplicationContext(ApplicationContext applicationContext) {
    mAppCtx = applicationContext;
    this.mInvocationRecords.add("setBeanName-" + mSystemId);
    System.err.println("setBeanName-" + mSystemId);   
  }
  
  /**
   * Is this method within a synchronization block ????
   */
  public void setBeanName(String beanName) {
    mBeanName = beanName;
    mInvocationRecords.add("setApplicationContext-" + mSystemId);
    System.err.println("setApplicationContext-" + mSystemId);    
  }
  
  public void destroy() {
    synchronized(this) {  // to avoid UnlockedSharedObjectException, this method is not in a synchronization block ??? 
      mInvocationRecords.add("destroy-" + mSystemId);
    }
  }
  
  public void closeAppCtx() {
    ((AbstractApplicationContext)mAppCtx).close();
  }

  public List getProp() {
    return mProp;
  }

  public void setProp(List prop) {
    mProp = prop;
  }
}
