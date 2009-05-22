/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.spring.bean;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.AbstractApplicationContext;

import com.tc.aspectwerkz.proxy.Uuid;

import java.util.ArrayList;
import java.util.List;

public class LifeCycleBean implements InitializingBean, ApplicationContextAware, BeanNameAware, DisposableBean,
    ILifeCycle {
  private transient long               mSystemId;

  private List                         mProp;

  private List                         mInvocationRecords = new ArrayList();
  private transient ApplicationContext mAppCtx            = null;
  private transient String             mBeanName          = null;

  public LifeCycleBean() {
    mSystemId = System.identityHashCode(this) + Uuid.newUuid();
  }

  public long getSystemId() {
    return mSystemId;
  }

  String getMBeanName() {
    return mBeanName;
  }

  synchronized public List getInvocationRecords() {
    return mInvocationRecords;
  }

  /**
   * Is this method within a synchronization block ????
   */
  public void afterPropertiesSet() {
    // this property will be initialized and shared
    addProp("" + mSystemId);
    addRecord("afterPropertiesSet-" + mSystemId);
    System.err.println("afterPropertiesSet-" + mSystemId);
  }

  /**
   * Is this method within a synchronization block ????
   */
  public void setApplicationContext(ApplicationContext applicationContext) {
    mAppCtx = applicationContext;
    addRecord("setBeanName-" + mSystemId);
    System.err.println("setBeanName-" + mSystemId);
  }

  /**
   * Is this method within a synchronization block ????
   */
  public synchronized void setBeanName(String beanName) {
    mBeanName = beanName;
    addRecord("setApplicationContext-" + mSystemId);
    System.err.println("setApplicationContext-" + mSystemId);
  }

  public void destroy() {
    addRecord("destroy-" + mSystemId);
  }

  public void closeAppCtx() {
    ((AbstractApplicationContext) mAppCtx).close();
  }

  public synchronized List getProp() {
    return mProp;
  }

  public synchronized void setProp(List prop) {
    mProp = prop;
  }

  public synchronized void addProp(String prop) {
    mProp.add(prop);
  }

  public synchronized void addRecord(String record) {
    mInvocationRecords.add(record);
  }
}
