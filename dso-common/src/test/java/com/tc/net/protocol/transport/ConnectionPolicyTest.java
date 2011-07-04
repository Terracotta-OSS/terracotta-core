/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.util.Assert;

import junit.framework.TestCase;

public class ConnectionPolicyTest extends TestCase {
  private ConnectionPolicy policy;

  public void tests() throws Exception {
    policy = new ConnectionPolicyImpl(2);

    policy.connectClient(new ConnectionID(1));
    assertFalse(policy.isMaxConnectionsReached());

    policy.connectClient(new ConnectionID(2));
    assertTrue(policy.isMaxConnectionsReached());

    // this is not accepted
    policy.connectClient(new ConnectionID(3));
    assertTrue(policy.isMaxConnectionsReached());

    policy.clientDisconnected(new ConnectionID(2));
    assertFalse(policy.isMaxConnectionsReached());

    policy.clientDisconnected(new ConnectionID(2));
    assertFalse(policy.isMaxConnectionsReached());

    policy.clientDisconnected(new ConnectionID(1));
    assertFalse(policy.isMaxConnectionsReached());

    policy.connectClient(new ConnectionID(3));
    assertFalse(policy.isMaxConnectionsReached());

    policy.connectClient(new ConnectionID(1));
    assertTrue(policy.isMaxConnectionsReached());

    policy.clientDisconnected(new ConnectionID(3));
    policy.clientDisconnected(new ConnectionID(1));

    boolean client9 = policy.connectClient(new ConnectionID(9));
    Assert.assertTrue(client9);

    boolean client10 = policy.connectClient(new ConnectionID(10));
    Assert.assertTrue(client10);
    assertTrue(policy.isMaxConnectionsReached());

    boolean client9_inst2 = policy.connectClient(new ConnectionID(9));
    Assert.assertTrue(client9_inst2);

    boolean client10_inst2 = policy.connectClient(new ConnectionID(10));
    Assert.assertTrue(client10_inst2);
    assertTrue(policy.isMaxConnectionsReached());

    policy.clientDisconnected(new ConnectionID(10));
    assertFalse(policy.isMaxConnectionsReached());

  }
}
