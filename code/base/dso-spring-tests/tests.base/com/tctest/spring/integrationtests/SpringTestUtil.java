/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.spring.integrationtests;

import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanFactory;

import com.tc.test.TCTestCase;
import com.tc.test.TestConfigObject;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;

import junit.framework.TestCase;

public class SpringTestUtil {
  /**
   * Add spring variant lib and other needed classes/jars to the test WAR file
   */
  public static DeploymentBuilder additionalDependencies(DeploymentBuilder builder) {
    // All spring tests need these I guess
    builder.addDirectoryOrJARContainingClass(LogFactory.class); // commons-logging
    builder.addDirectoryOrJARContainingClass(Logger.class); // log4j

    // Couple tests fail using Spring 2.5.4 w/o these lines
    builder.addDirectoryOrJARContainingClass(TCTestCase.class);
    builder.addDirectoryOrJARContainingClass(TestCase.class);

    builder.addDirectoryOrJARContainingClassOfSelectedVersion(BeanFactory.class,
                                                              new String[] { TestConfigObject.SPRING_VARIANT }); // springframework

    return builder;
  }
}
