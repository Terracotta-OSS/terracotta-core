/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.test;

import com.tc.config.schema.builder.SpringApplicationContextConfigBuilder;
import com.tc.config.schema.builder.SpringBeanConfigBuilder;


public class SpringApplicationContextConfigBuilderImpl extends BaseConfigBuilder implements SpringApplicationContextConfigBuilder {

  protected SpringApplicationContextConfigBuilderImpl() {
    super(5, new String[] { "paths", "beans" });
  }

  public static SpringApplicationContextConfigBuilder newMinimalInstance() {
    return new SpringApplicationContextConfigBuilderImpl();
  }

  public String toString() {
    return elementGroup("application-context", new String[] { "paths", "beans" });
  }

  public void setPaths(String[] paths) {
    SpringApplicationContextPathConfigBuilder[] pcb = new SpringApplicationContextPathConfigBuilder[paths.length];
    for (int i = 0; i < paths.length; i++) {
      String path = paths[i];
      pcb[i] = new SpringApplicationContextPathConfigBuilderImpl(path);
    }
    setPaths(pcb);
  }

  public void setPaths(SpringApplicationContextPathConfigBuilder[] paths) {
    setProperty("paths", selfTaggingArray(paths));
  }

  public SpringBeanConfigBuilder addBean(String beanName) {
    if (!isSet("beans")) {
      setProperty("beans", selfTaggingArray(new SpringBeanConfigBuilder[0]));
    }
    SpringBeanConfigBuilder springBean = new SpringBeanConfigBuilderImpl(beanName);
    SpringBeanConfigBuilder[] existingBeans = (SpringBeanConfigBuilder[]) ((SelfTaggingArray)getRawProperty("beans")).values();
    SpringBeanConfigBuilder[] newBeans = new SpringBeanConfigBuilder[existingBeans.length + 1];
    System.arraycopy(existingBeans, 0, newBeans, 0,  existingBeans.length);
    newBeans[existingBeans.length] = springBean;
    setProperty("beans", selfTaggingArray(newBeans));
    return springBean;
  }

    
  // beans

}
