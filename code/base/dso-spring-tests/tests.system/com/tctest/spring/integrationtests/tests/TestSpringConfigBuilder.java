/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import com.tc.config.schema.builder.SpringApplicationConfigBuilder;
import com.tc.config.schema.builder.SpringApplicationContextConfigBuilder;
import com.tc.config.schema.builder.SpringBeanConfigBuilder;
import com.tc.config.schema.builder.SpringConfigBuilder;
import com.tc.config.schema.test.TerracottaConfigBuilder;

import junit.framework.TestCase;

public class TestSpringConfigBuilder extends TestCase {

  public void test() {
    System.out.println(buildTCConfig().toString());
  }
  
  public TerracottaConfigBuilder buildTCConfig() {
    TerracottaConfigBuilder builder = TerracottaConfigBuilder.newMinimalInstance();

    SpringConfigBuilder b = builder.getApplication().getSpring();
    SpringApplicationConfigBuilder application = b.getApplications()[0];
    application.setName("test-singleton");
    SpringApplicationContextConfigBuilder applicationContext = application.getApplicationContexts()[0];
    applicationContext.setPaths(new String[]{"*.xml"});
    SpringBeanConfigBuilder bean = applicationContext.addBean("singleton");
    return builder;

  }
}
