/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests;

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
    return addSpringVariant(sm.makeDeploymentBuilder());
  }

  public DeploymentBuilder makeDeploymentBuilder(String warFileName) {
    return addSpringVariant(sm.makeDeploymentBuilder(warFileName));
  }

  private DeploymentBuilder addSpringVariant(DeploymentBuilder builder) {
    return builder.addDirectoryOrJARContainingClassOfSelectedVersion(BeanFactory.class,
                                                              new String[] { TestConfigObject.SPRING_VARIANT }); // springframework
  }
  
}
