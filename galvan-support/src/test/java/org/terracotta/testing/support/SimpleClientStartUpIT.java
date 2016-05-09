/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.terracotta.testing.support;

import org.terracotta.connection.Connection;
import org.terracotta.passthrough.IClientTestEnvironment;
import org.terracotta.passthrough.IClusterControl;


/**
 * A trivial test which just starts 2 clients, which do nothing and exit.  No exceptions are expected.
 */
public class SimpleClientStartUpIT extends MultiProcessGalvanTest {
  @Override
  public int getClientsToStart() {
    return 2;
  }

  @Override
  public void runTest(IClientTestEnvironment env, IClusterControl control, Connection connection) throws Throwable {
    int clientIndex = env.getThisClientIndex();
    System.out.println("Running in client: " + clientIndex);
  }
}
