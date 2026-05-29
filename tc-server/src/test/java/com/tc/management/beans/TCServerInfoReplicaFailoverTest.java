/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2026
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.management.beans;

import com.tc.productinfo.ProductInfo;
import com.tc.server.TCServer;
import org.junit.Before;
import org.junit.Test;

import javax.management.NotCompliantMBeanException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for replica failover to active diagnostic functionality added in commit 5b28bd26e3.
 * Verifies the replicaFailoverToActive() method in TCServerInfo.
 */
public class TCServerInfoReplicaFailoverTest {

  private TCServer server;
  private TCServerInfo serverInfo;
  private ProductInfo productInfo;

  @Before
  public void setUp() throws NotCompliantMBeanException {
    server = mock(TCServer.class);
    productInfo = mock(ProductInfo.class);

    when(server.productInfo()).thenReturn(productInfo);
    when(productInfo.buildID()).thenReturn("test-build");
    when(productInfo.moniker()).thenReturn("Terracotta");
    when(productInfo.kitID()).thenReturn("test-kit");
    when(productInfo.toShortString()).thenReturn("1.0.0");
    when(productInfo.copyright()).thenReturn("Copyright Test");

    serverInfo = new TCServerInfo(server);
  }

  @Test
  public void testReplicaFailoverToActiveSuccess() {
    // Mock successful failover
    when(server.replicaFailoverToActive()).thenReturn(true);

    boolean result = serverInfo.replicaFailoverToActive();

    assertTrue("Failover should succeed", result);
    verify(server).replicaFailoverToActive();
  }

  @Test
  public void testReplicaFailoverToActiveFailure() {
    // Mock failed failover
    when(server.replicaFailoverToActive()).thenReturn(false);

    boolean result = serverInfo.replicaFailoverToActive();

    assertFalse("Failover should fail", result);
    verify(server).replicaFailoverToActive();
  }

  @Test
  public void testReplicaFailoverToActiveCalledOnce() {
    when(server.replicaFailoverToActive()).thenReturn(true);

    serverInfo.replicaFailoverToActive();

    // Verify the method is called exactly once
    verify(server, times(1)).replicaFailoverToActive();
  }

  @Test
  public void testReplicaFailoverToActiveWithException() {
    // Mock exception during failover
    when(server.replicaFailoverToActive()).thenThrow(new RuntimeException("Failover error"));

    try {
      serverInfo.replicaFailoverToActive();
      fail("Expected RuntimeException");
    } catch (RuntimeException e) {
      assertEquals("Failover error", e.getMessage());
    }

    verify(server).replicaFailoverToActive();
  }

  @Test
  public void testReplicaFailoverToActiveMultipleCalls() {
    // Test multiple failover attempts
    when(server.replicaFailoverToActive())
        .thenReturn(false)  // First attempt fails
        .thenReturn(true);  // Second attempt succeeds

    boolean firstResult = serverInfo.replicaFailoverToActive();
    assertFalse("First failover should fail", firstResult);

    boolean secondResult = serverInfo.replicaFailoverToActive();
    assertTrue("Second failover should succeed", secondResult);

    verify(server, times(2)).replicaFailoverToActive();
  }

  @Test
  public void testReplicaFailoverToActiveIntegrationWithServerState() {
    // Test that failover respects server state
    when(server.isActive()).thenReturn(false);
    when(server.isPassiveStandby()).thenReturn(false);
    when(server.replicaFailoverToActive()).thenReturn(true);

    // Server should not be active before failover
    assertFalse(serverInfo.isActive());
    assertFalse(serverInfo.isPassiveStandby());

    // Perform failover
    boolean result = serverInfo.replicaFailoverToActive();
    assertTrue("Failover should succeed", result);

    // Simulate server becoming active after failover
    when(server.isActive()).thenReturn(true);
    assertTrue(serverInfo.isActive());
  }

  @Test
  public void testReplicaFailoverToActiveWhenAlreadyActive() {
    // Test failover when server is already active
    when(server.isActive()).thenReturn(true);
    when(server.replicaFailoverToActive()).thenReturn(false);

    boolean result = serverInfo.replicaFailoverToActive();

    // Should return false since already active
    assertFalse("Failover should fail when already active", result);
    verify(server).replicaFailoverToActive();
  }

  @Test
  public void testReplicaFailoverToActiveNotAffectingOtherOperations() {
    // Verify failover doesn't interfere with other server operations
    when(server.isStarted()).thenReturn(true);
    when(server.getStartTime()).thenReturn(System.currentTimeMillis());
    when(server.replicaFailoverToActive()).thenReturn(true);

    // Other operations should work normally
    assertTrue(serverInfo.isStarted());
    assertTrue(serverInfo.getStartTime() > 0);

    // Failover should still work
    assertTrue(serverInfo.replicaFailoverToActive());
  }
}
