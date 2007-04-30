/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests;

import org.springframework.beans.factory.BeanFactory;

import com.tc.test.TestConfigObject;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest.TwoServerTestSetup;

import java.io.IOException;

public abstract class SpringTwoServerTestSetup extends TwoServerTestSetup {

  protected SpringTwoServerTestSetup(Class testClass, String tcConfigFile, String context) {
    super(testClass, tcConfigFile, context);
  }

  public DeploymentBuilder makeDeploymentBuilder() throws IOException {
    return addSpringVariant(getServerManager().makeDeploymentBuilder());
  }

  public DeploymentBuilder makeDeploymentBuilder(String warFileName) {
    return addSpringVariant(getServerManager().makeDeploymentBuilder(warFileName));
  }

  private DeploymentBuilder addSpringVariant(DeploymentBuilder builder) {
    return builder.addDirectoryOrJARContainingClassOfSelectedVersion(BeanFactory.class,
                                                              new String[] { TestConfigObject.SPRING_VARIANT }); // springframework
  }

}
