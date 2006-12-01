/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.test;

import com.tc.config.schema.builder.SpringBeanConfigBuilder;

import java.util.Collections;

public class SpringBeanConfigBuilderImpl extends BaseConfigBuilder implements SpringBeanConfigBuilder {

  private final String beanName;

  public SpringBeanConfigBuilderImpl(String beanName) {
    super(8, new String[] {"beanName"});
    this.beanName = beanName;
  }

  public String toString() {
    return openElement("bean", Collections.singletonMap("name", beanName))
            + closeElement("bean");
  }
}
