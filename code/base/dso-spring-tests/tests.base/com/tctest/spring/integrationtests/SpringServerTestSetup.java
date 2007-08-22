/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests;

import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanFactory;

import com.tc.test.TestConfigObject;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.ServerTestSetup;

import java.io.IOException;

public class SpringServerTestSetup extends ServerTestSetup {

  public SpringServerTestSetup(Class testClass) {
    super(testClass);
  }

  public DeploymentBuilder makeDeploymentBuilder() throws IOException {
    return addSpringVariant(getServerManager().makeDeploymentBuilder());
  }

  public DeploymentBuilder makeDeploymentBuilder(String warFileName) {
    return addSpringVariant(getServerManager().makeDeploymentBuilder(warFileName));
  }

  private DeploymentBuilder addSpringVariant(DeploymentBuilder builder) {
    // All spring tests need these I guess
    builder.addDirectoryOrJARContainingClass(LogFactory.class); // commons-logging
    builder.addDirectoryOrJARContainingClass(Logger.class); // log4j

    return builder.addDirectoryOrJARContainingClassOfSelectedVersion(BeanFactory.class,
                                                              new String[] { TestConfigObject.SPRING_VARIANT }); // springframework
  }

}
