/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;


public class InitializingSingleton implements InitializingBean, BeanNameAware, BeanFactoryAware {

  private Recorder recorder;
  private String name;
  private transient BeanFactory beanFactory;
  
  public void setRecorder(Recorder recorder) {
    System.err.println(System.identityHashCode(this) + "Calling setRecroder(" + recorder + ")");
    this.recorder = recorder;
  }
  
  public void setBeanName(String name) {
    this.name = name;
  }
  
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    this.beanFactory = beanFactory;
  }
  
  public void afterPropertiesSet() throws Exception {
    System.err.println(System.identityHashCode(this) + " Calling afterPropertiesSet()");
    System.err.println(System.identityHashCode(this) + " has recorder " + recorder);
    recorder.addValue(name);
  }
}

