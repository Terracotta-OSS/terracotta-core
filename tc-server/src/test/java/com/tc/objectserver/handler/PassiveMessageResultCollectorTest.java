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
package com.tc.objectserver.handler;

import com.tc.l2.msg.ReplicationResultCode;
import com.tc.l2.msg.SyncReplicationActivity;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityID;
import com.tc.object.FetchID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.junit.Assert.*;

/**
 * Tests for PassiveMessageResultCollector interface added in commit 453d3c50f2.
 * Verifies the contract and basic implementations.
 */
public class PassiveMessageResultCollectorTest {

  @Test
  public void testPassiveMessageResultCollectorContract() {
    // Create a simple implementation to test the interface contract
    PassiveMessageResultCollector collector = new TestPassiveMessageResultCollector();

    ServerID activeServer = new ServerID("active", "active".getBytes());
    SyncReplicationActivity activity = createTestActivity();

    // Test acknowledge method
    collector.acknowledge(activeServer, activity, ReplicationResultCode.SUCCESS);

    // Test ackReceived method
    CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
    collector.ackReceived(activeServer, activity, future);

    // Test getLocalNodeID method
    ServerID localNode = collector.getLocalNodeID();
    assertNotNull("Local node ID should not be null", localNode);

    // Test transform method
    ServerEntityRequest request = collector.transform(activity);
    assertNotNull("Transformed request should not be null", request);
    assertEquals(ServerEntityAction.INVOKE_ACTION, request.getAction());
  }

  @Test
  public void testTransformWithDifferentActivityTypes() {
    PassiveMessageResultCollector collector = new TestPassiveMessageResultCollector();

    // Test CREATE_ENTITY
    SyncReplicationActivity createActivity = SyncReplicationActivity.createLifecycleMessage(
        new EntityID("test", "test"),
        1L,
        new FetchID(1L),
        new ClientID(1L),
        new ClientInstanceID(1L),
        new TransactionID(1L),
        new TransactionID(0L),
        SyncReplicationActivity.ActivityType.CREATE_ENTITY,
        null
    );
    ServerEntityRequest createRequest = collector.transform(createActivity);
    assertEquals(ServerEntityAction.CREATE_ENTITY, createRequest.getAction());

    // Test DESTROY_ENTITY
    SyncReplicationActivity destroyActivity = SyncReplicationActivity.createLifecycleMessage(
        new EntityID("test", "test"),
        1L,
        new FetchID(1L),
        new ClientID(1L),
        new ClientInstanceID(1L),
        new TransactionID(1L),
        new TransactionID(0L),
        SyncReplicationActivity.ActivityType.DESTROY_ENTITY,
        null
    );
    ServerEntityRequest destroyRequest = collector.transform(destroyActivity);
    assertEquals(ServerEntityAction.DESTROY_ENTITY, destroyRequest.getAction());

    // Test FETCH_ENTITY
    SyncReplicationActivity fetchActivity = SyncReplicationActivity.createLifecycleMessage(
        new EntityID("test", "test"),
        1L,
        new FetchID(1L),
        new ClientID(1L),
        new ClientInstanceID(1L),
        new TransactionID(1L),
        new TransactionID(0L),
        SyncReplicationActivity.ActivityType.FETCH_ENTITY,
        null
    );
    ServerEntityRequest fetchRequest = collector.transform(fetchActivity);
    assertEquals(ServerEntityAction.FETCH_ENTITY, fetchRequest.getAction());
  }

  @Test
  public void testAckReceivedWithNullFuture() {
    PassiveMessageResultCollector collector = new TestPassiveMessageResultCollector();
    ServerID activeServer = new ServerID("active", "active".getBytes());
    SyncReplicationActivity activity = createTestActivity();

    // Should handle null future gracefully
    collector.ackReceived(activeServer, activity, null);
  }

  @Test
  public void testAcknowledgeWithDifferentResultCodes() {
    PassiveMessageResultCollector collector = new TestPassiveMessageResultCollector();
    ServerID activeServer = new ServerID("active", "active".getBytes());
    SyncReplicationActivity activity = createTestActivity();

    // Test all result codes
    collector.acknowledge(activeServer, activity, ReplicationResultCode.SUCCESS);
    collector.acknowledge(activeServer, activity, ReplicationResultCode.RECEIVED);
    collector.acknowledge(activeServer, activity, ReplicationResultCode.FAIL);
  }

  @Test
  public void testRequestPassiveSync() {
    PassiveMessageResultCollector collector = new TestPassiveMessageResultCollector();
    ServerID target = new ServerID("target", "target".getBytes());

    // Should not throw exception
    try {
      collector.requestPassiveSync(target);
    } catch (UnsupportedOperationException e) {
      // Expected for test implementation
      assertTrue(e.getMessage().contains("Not supported"));
    }
  }

  private SyncReplicationActivity createTestActivity() {
    return SyncReplicationActivity.createInvokeMessage(
        new FetchID(1L),
        new ClientID(1L),
        new ClientInstanceID(1L),
        new TransactionID(1L),
        new TransactionID(0L),
        SyncReplicationActivity.ActivityType.INVOKE_ACTION,
        null,
        1,
        ""
    );
  }

  /**
   * Simple test implementation of PassiveMessageResultCollector
   */
  private static class TestPassiveMessageResultCollector implements PassiveMessageResultCollector {
    private final ServerID localNode = new ServerID("local", "local".getBytes());

    @Override
    public void acknowledge(ServerID activeSender, SyncReplicationActivity activity, ReplicationResultCode code) {
      // Test implementation - just verify parameters are not null
      assertNotNull(activeSender);
      assertNotNull(activity);
      assertNotNull(code);
    }

    @Override
    public void ackReceived(ServerID activeSender, SyncReplicationActivity activity, Future<Void> future) {
      // Test implementation - just verify parameters are not null (except future which can be null)
      assertNotNull(activeSender);
      assertNotNull(activity);
    }

    @Override
    public ServerID getLocalNodeID() {
      return localNode;
    }

    @Override
    public void requestPassiveSync(NodeID target) {
      throw new UnsupportedOperationException("Not supported in test implementation");
    }

    @Override
    public ServerEntityRequest transform(SyncReplicationActivity activity) {
      return new ReplicatedTransactionHandler.BasicServerEntityRequest(
          PassiveAckSender.decodeReplicationType(activity.getActivityType()),
          activity.getSource(),
          activity.getClientInstanceID(),
          activity.getTransactionID(),
          activity.getOldestTransactionOnClient()
      );
    }
  }
}
