/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.testing.support;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
  public Set<Path> getExtraServerJarPaths() {
    // We expect the client code to be in the same location as the harness.
    return Collections.emptySet();
  }

  @Override
  public String getServiceConfigXMLSnippet() {
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
  public List<BasicTestClusterConfiguration> getRunConfigurations() {
    return Collections.singletonList(new BasicTestClusterConfiguration("Test", 1));
  }


  @Override
  public ITestMaster<BasicTestClusterConfiguration> getTestMaster() {
    return this;
  }

  @Override
  public void runSetup(IClientTestEnvironment env, IClusterControl control) {
    // These tests generally don't care about this.
  }

  @Override
  public void runDestroy(IClientTestEnvironment env, IClusterControl control) {
    // These tests generally don't care about this.
  }
}
