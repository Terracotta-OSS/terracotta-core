/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.spring.integrationtests;

import com.tc.test.TestConfigObject;
import com.tc.test.server.appserver.deployment.AbstractDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;

public class SpringDeploymentTest extends AbstractDeploymentTest {

  public SpringDeploymentTest() {
    TestConfigObject.getInstance().setSpringTest(true);
  }

  protected DeploymentBuilder makeDeploymentBuilder(String warFileName) {
    return SpringTestUtil.additionalDependencies(getServerManager().makeDeploymentBuilder(warFileName));
  }

}
