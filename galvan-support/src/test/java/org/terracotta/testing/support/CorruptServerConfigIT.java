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
 * This test overrides the normal config options of the server, resulting in a corrupt config.  The server will fail to even
 * start.
 * 1 Client is started, who does nothing.
 */
public class CorruptServerConfigIT extends MultiProcessGalvanTest {
  @Override
  public int getClientsToStart() {
    return 1;
  }

  @Override
  public String getEntityConfigXMLSnippet() {
    return "Bogus<String<";
  }

  @Override
  public void interpretResult(Throwable error) throws Throwable {
    // We are expecting an error, so throw if it is missing.
    if (null == error) {
      throw new Exception("Test should have failed");
    }
  }

  @Override
  public void runTest(IClientTestEnvironment env, IClusterControl control, Connection connection) throws Throwable {
    int clientIndex = env.getThisClientIndex();
    System.out.println("Running in client: " + clientIndex);
  }
}
