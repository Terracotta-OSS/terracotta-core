/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.terracotta.testing.support;

import java.util.Collections;
import java.util.List;

import org.terracotta.connection.Connection;
import org.terracotta.passthrough.IClientTestEnvironment;
import org.terracotta.passthrough.IClusterControl;
import org.terracotta.passthrough.ICommonTest;
import org.terracotta.testing.api.BasicTestClusterConfiguration;
import org.terracotta.testing.api.ITestMaster;


/**
 * A class to test some of Galvan's basic functionality when running multi-process tests.
 * Tests which want to interact with galvan from the perspective of a running client, can implement this abstract class in order to do so.
 */
public abstract class MultiProcessGalvanTest extends BasicHarnessTest implements ITestMaster<BasicTestClusterConfiguration>, ICommonTest {
  public MultiProcessGalvanTest() {
    this.setName(this.getClass().getSimpleName());
  }

  @Override
  public String getConfigNamespaceSnippet() {
    return "";
  }

  @Override
  public List<String> getExtraServerJarPaths() {
    // We expect the client code to be in the same location as the harness.
    return Collections.emptyList();
  }

  @Override
  public String getServiceConfigXMLSnippet() {
    return "";
  }

  @Override
  public String getEntityConfigXMLSnippet() {
    return "";
  }

  @Override
  public String getTestClassName() {
    return this.getClass().getCanonicalName();
  }

  @Override
  public String getClientErrorHandlerClassName() {
    // This implementation doesn't have a client error handler.
    return null;
  }

  @Override
  public boolean isRestartable() {
    return false;
  }

  @Override
  public List<BasicTestClusterConfiguration> getRunConfigurations() {
    return Collections.singletonList(new BasicTestClusterConfiguration("Test", 1));
  }


  @Override
  public ITestMaster<BasicTestClusterConfiguration> getTestMaster() {
    return this;
  }

  @Override
  public void runSetup(IClientTestEnvironment env, IClusterControl control, Connection connection) {
    // These tests generally don't care about this.
  }

  @Override
  public void runDestroy(IClientTestEnvironment env, IClusterControl control, Connection connection) {
    // These tests generally don't care about this.
  }
}
