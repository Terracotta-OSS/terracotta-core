/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.terracotta.testing.support;

import org.junit.Assert;
import org.terracotta.connection.Connection;
import org.terracotta.passthrough.IClientTestEnvironment;
import org.terracotta.passthrough.IClusterControl;


/**
 * A trivial test which just starts 2 clients which verify client counts, and exit.  No exceptions are expected.
 */
public class SimpleClientStartUpIT extends MultiProcessGalvanTest {
  private static final int CLIENT_COUNT = 2;

  @Override
  public int getClientsToStart() {
    return CLIENT_COUNT;
  }

  @Override
  public void runSetup(IClientTestEnvironment env, IClusterControl control, Connection connection) {
    // Just verify that the client counts are expected.
    Assert.assertTrue(CLIENT_COUNT == env.getTotalClientCount());
    Assert.assertTrue(0 == env.getThisClientIndex());
  }

  @Override
  public void runDestroy(IClientTestEnvironment env, IClusterControl control, Connection connection) {
    // These tests generally don't care about this.
    Assert.assertTrue(CLIENT_COUNT == env.getTotalClientCount());
    Assert.assertTrue(0 == env.getThisClientIndex());
  }

  @Override
  public void runTest(IClientTestEnvironment env, IClusterControl control, Connection connection) throws Throwable {
    int clientIndex = env.getThisClientIndex();
    Assert.assertTrue(clientIndex >= 0);
    Assert.assertTrue(clientIndex < CLIENT_COUNT);
    Assert.assertTrue(CLIENT_COUNT == env.getTotalClientCount());
    System.out.println("Running in client: " + clientIndex);
  }
}
