/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests;

import org.springframework.beans.factory.BeanFactory;

import com.tc.test.TestConfigObject;
import com.tc.test.server.appserver.deployment.AbstractDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;

public class SpringDeploymentTest extends AbstractDeploymentTest {

  protected DeploymentBuilder makeDeploymentBuilder(String warFileName) {
    DeploymentBuilder builder = serverManager.makeDeploymentBuilder(warFileName);
    
    builder.addDirectoryOrJARContainingClassOfSelectedVersion(BeanFactory.class,
                                                              new String[] { TestConfigObject.SPRING_VARIANT }); // springframework
    
    return builder;
  }
  
}
