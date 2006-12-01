/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.transport;

import junit.framework.TestCase;

public class ConnectionPolicyTest extends TestCase {
  private ConnectionPolicy policy;
  
  public void tests() throws Exception {
    policy = new ConnectionPolicyImpl(-1);

    policy.clientConnected();
    assertFalse(policy.maxConnectionsExceeded());
  }
}
