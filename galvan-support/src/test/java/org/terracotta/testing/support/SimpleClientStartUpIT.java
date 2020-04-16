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
 */
package org.terracotta.testing.support;

import org.junit.Assert;
import org.terracotta.passthrough.IClientTestEnvironment;
import org.terracotta.passthrough.IClusterControl;

import java.util.Properties;


/**
 * A trivial test which just starts 2 clients which verify client counts, and exit.  No exceptions are expected.
 */
public class SimpleClientStartUpIT extends MultiProcessGalvanTest {
  private static final int CLIENT_COUNT = 2;

  @Override
  public Properties getTcProperties() {
    Properties tcProperties = new Properties();
    tcProperties.put("server.entity.processor.threads", "16");
    return tcProperties;
  }

  @Override
  public int getClientsToStart() {
    return CLIENT_COUNT;
  }

  @Override
  public void runSetup(IClientTestEnvironment env, IClusterControl control) {
    // Just verify that the client counts are expected.
    Assert.assertTrue(CLIENT_COUNT == env.getTotalClientCount());
    Assert.assertTrue(0 == env.getThisClientIndex());
  }

  @Override
  public void runDestroy(IClientTestEnvironment env, IClusterControl control) {
    // These tests generally don't care about this.
    Assert.assertTrue(CLIENT_COUNT == env.getTotalClientCount());
    Assert.assertTrue(0 == env.getThisClientIndex());
  }

  @Override
  public void runTest(IClientTestEnvironment env, IClusterControl control) throws Throwable {
    int clientIndex = env.getThisClientIndex();
    Assert.assertTrue(clientIndex >= 0);
    Assert.assertTrue(clientIndex < CLIENT_COUNT);
    Assert.assertTrue(CLIENT_COUNT == env.getTotalClientCount());
    System.out.println("Running in client: " + clientIndex);
  }
}
