/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.spring.integrationtests;

import com.tc.test.TestConfigObject;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.ServerTestSetup;

import java.io.IOException;

public class SpringServerTestSetup extends ServerTestSetup {

  public SpringServerTestSetup(Class testClass) {
    super(testClass);
    TestConfigObject.getInstance().setSpringTest(true);
  }

  public DeploymentBuilder makeDeploymentBuilder() throws IOException {
    return SpringTestUtil.additionalDependencies(getServerManager().makeDeploymentBuilder());
  }

  public DeploymentBuilder makeDeploymentBuilder(String warFileName) {
    return SpringTestUtil.additionalDependencies(getServerManager().makeDeploymentBuilder(warFileName));
  }
}
