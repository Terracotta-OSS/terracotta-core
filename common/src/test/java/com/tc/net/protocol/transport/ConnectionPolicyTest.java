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
package com.tc.net.protocol.transport;

import org.junit.jupiter.api.Test;

import com.tc.util.Assert;
import com.tc.util.ProductID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConnectionPolicyTest {
  private ConnectionPolicy policy;

  @Test
  public void tests() throws Exception {
    policy = new ConnectionPolicyImpl(2);

    policy.connectClient(new ConnectionID("jvm1", 1));
    assertFalse(policy.isMaxConnectionsReached());

    policy.connectClient(new ConnectionID("jvm2", 2));
    assertTrue(policy.isMaxConnectionsReached());

    // this is not accepted
    policy.connectClient(new ConnectionID("jvm3", 3));
    assertTrue(policy.isMaxConnectionsReached());

    policy.clientDisconnected(new ConnectionID("jvm2", 2));
    assertFalse(policy.isMaxConnectionsReached());

    policy.clientDisconnected(new ConnectionID("jvm2", 2));
    assertFalse(policy.isMaxConnectionsReached());

    policy.clientDisconnected(new ConnectionID("jvm1", 1));
    assertFalse(policy.isMaxConnectionsReached());

    policy.connectClient(new ConnectionID("jvm3", 3));
    assertFalse(policy.isMaxConnectionsReached());

    policy.connectClient(new ConnectionID("jvm1", 1));
    assertTrue(policy.isMaxConnectionsReached());

    policy.clientDisconnected(new ConnectionID("jvm3", 3));
    policy.clientDisconnected(new ConnectionID("jvm1", 1));

    boolean client9 = policy.connectClient(new ConnectionID("jvm9", 9));
    Assert.assertTrue(client9);

    boolean client10 = policy.connectClient(new ConnectionID("jvm10", 10));
    Assert.assertTrue(client10);
    assertTrue(policy.isMaxConnectionsReached());

    boolean client9_inst2 = policy.connectClient(new ConnectionID("jvm9", 9));
    Assert.assertTrue(client9_inst2);

    boolean client10_inst2 = policy.connectClient(new ConnectionID("jvm10", 10));
    Assert.assertTrue(client10_inst2);
    assertTrue(policy.isMaxConnectionsReached());

    policy.clientDisconnected(new ConnectionID("jvm10", 10));
    assertFalse(policy.isMaxConnectionsReached());

    // "jvm9" is still connected at this point.
    // make sure we can add multiple distinct clients from the same jvm without
    // using a license...

    boolean client9b = policy.connectClient(new ConnectionID("jvm9", 9));
    Assert.assertTrue(client9b);

    boolean client9c = policy.connectClient(new ConnectionID("jvm9", 99));
    Assert.assertTrue(client9c);

    boolean client9d = policy.connectClient(new ConnectionID("jvm9", 999));
    Assert.assertTrue(client9d);

    boolean client10again = policy.connectClient(new ConnectionID("jvm10", 10));
    Assert.assertTrue(client10again);
    assertTrue(policy.isMaxConnectionsReached());

    // make sure now that max connections is reached, the same jvms can still
    // make new connections...

    boolean client9e = policy.connectClient(new ConnectionID("jvm9", 9999));
    Assert.assertTrue(client9e);

    boolean client10again2 = policy.connectClient(new ConnectionID("jvm10", 100));
    Assert.assertTrue(client10again2);

    // and make sure we cannot add a client from an addition jvm...

    boolean client11 = policy.connectClient(new ConnectionID("jvm11", 11));
    Assert.assertFalse(client11);

    client11 = policy.isConnectAllowed(new ConnectionID("jvm11", 11));
    Assert.assertFalse(client11);
  }

  @Test
  public void testInternalClients() throws Exception {
    policy = new ConnectionPolicyImpl(1);
    assertTrue(policy.connectClient(new ConnectionID("foo", 1)));
    assertTrue(policy.isConnectAllowed(new ConnectionID("bar", 2, null, null, ProductID.DIAGNOSTIC)));
    assertTrue(policy.connectClient(new ConnectionID("bar", 2, null, null, ProductID.DIAGNOSTIC)));
    policy.clientDisconnected(new ConnectionID("foo", 4, null, null, ProductID.DIAGNOSTIC));
    assertFalse(policy.connectClient(new ConnectionID("baz", 3)));
    assertTrue(policy.connectClient(new ConnectionID("redirect", -2)));
  }
}
