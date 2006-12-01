/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests;

import org.springframework.beans.factory.BeanFactory;

import com.tc.test.TestConfigObject;
import com.tctest.spring.bean.ISingleton;
import com.tctest.spring.integrationtests.framework.DeploymentBuilder;
import com.tctest.spring.integrationtests.framework.FileSystemPath;
import com.tctest.spring.integrationtests.framework.TempDirectoryUtil;
import com.tctest.spring.integrationtests.framework.WARBuilder;

import junit.framework.TestCase;

public class WARBuilderTest extends TestCase {


  public void test() throws Exception {
    DeploymentBuilder builder = new WARBuilder("foo.war", TempDirectoryUtil.getTempDirectory(getClass()), TestConfigObject.getInstance());
    populateWAR(builder);
    builder.makeDeployment();
  }

  public void testAnonymous() throws Exception {
    DeploymentBuilder builder = new WARBuilder(TempDirectoryUtil.getTempDirectory(getClass()), TestConfigObject.getInstance());
    populateWAR(builder);
    builder.makeDeployment();
  }

  private void populateWAR(DeploymentBuilder builder) {
    builder.addBeanDefinitionFile("classpath:/com/tctest/spring/beanfactory.xml");
    builder.addRemoteService("Singleton", "singleton", ISingleton.class);
    builder.addDirectoryOrJARContainingClass(getClass());
    builder.addDirectoryContainingResource("/tc-config-files/singleton-tc-config.xml");
  }

  public void testCalculatePathToDir() {
    FileSystemPath path = WARBuilder.calculatePathToClass(getClass());
    assertNotNull(path);
  }

//  public void testCalculatePathToDirProvidingString() {
//    String pathString = "C:\\repos\\kirkham\\code\\base\\dso-spring-tests\\build.eclipse\\tests.base.classes;C:\\workspace\\tc-main\\test;C:\\repos\\kirkham\\code\\base\\aspectwerkz\\lib\\ant.jar;C:\\repos\\kirkham\\code\\base\\dso-spring\\lib\\spring-1.2.8.jar";
//    String pathString = "C:/repos/kirkham/code/base/dso-spring/lib.abc.1.2.3/spring-1.2.8.jar";
//    String pathString = "C:/repos/kirkham/code/base/dso-spring-tests/build.eclipse/tests.base.classes;C:/workspace/tc-main/test;C:/repos/kirkham/code/base/aspectwerkz/lib/ant.jar;C:/repos/kirkham/code/base/dso-spring/lib/spring-1.2.8.jar";
//    FileSystemPath path = WARBuilder.calculatePathToClass(org.springframework.beans.factory.BeanFactory.class, pathString);
//    assertNotNull(path);
//  }

  public void testCalculatePathToJar() {
    FileSystemPath path = WARBuilder.calculatePathToClass(BeanFactory.class);
    assertNotNull(path);
  }
}
