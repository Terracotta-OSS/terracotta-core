/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SingletonFromSeparateContextFactoryBean extends AbstractFactoryBean implements InitializingBean {
  private String beanDefLocation = null;
  private ApplicationContext ctx = null;
  
  public Class getObjectType() {
    return Singleton.class;
  }
  
  public void afterPropertiesSet() throws Exception {
    ctx = new ClassPathXmlApplicationContext(
        new String[] { beanDefLocation });
    super.afterPropertiesSet();
  }

  protected Object createInstance() throws Exception {
    return ctx.getBean("singleton");
//    return new Singleton();
  }

  public String getBeanDefLocation() {
    return beanDefLocation;
  }

  public void setBeanDefLocation(String beanDefLocation) {
    this.beanDefLocation = beanDefLocation;
  }
}
