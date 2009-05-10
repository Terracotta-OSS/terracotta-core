/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.transport;

import junit.framework.TestCase;

public class ConnectionPolicyTest extends TestCase {
  private ConnectionPolicy policy;
  
  public void tests() throws Exception {
    policy = new ConnectionPolicyImpl(2);

    policy.connectClient(new ConnectionID(1));
    assertFalse(policy.isMaxConnectionsReached());
    
    policy.connectClient(new ConnectionID(2));
    assertTrue(policy.isMaxConnectionsReached());
    
    //this is not accepted
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
    
  }
}
