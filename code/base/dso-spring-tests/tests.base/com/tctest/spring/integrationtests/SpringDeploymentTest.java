/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.spring.integrationtests;

import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanFactory;

import com.tc.test.TestConfigObject;
import com.tc.test.server.appserver.deployment.AbstractDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;

public class SpringDeploymentTest extends AbstractDeploymentTest {

  protected DeploymentBuilder makeDeploymentBuilder(String warFileName) {
    DeploymentBuilder builder = getServerManager().makeDeploymentBuilder(warFileName);

    // All spring tests need these I guess
    builder.addDirectoryOrJARContainingClass(LogFactory.class); // commons-logging
    builder.addDirectoryOrJARContainingClass(Logger.class); // log4j

    builder.addDirectoryOrJARContainingClassOfSelectedVersion(BeanFactory.class,
                                                              new String[] { TestConfigObject.SPRING_VARIANT }); // springframework

    return builder;
  }

}
