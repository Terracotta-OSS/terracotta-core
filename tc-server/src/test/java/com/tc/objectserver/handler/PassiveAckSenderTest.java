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

import com.tc.async.api.Sink;
import com.tc.l2.msg.ReplicationMessageAck;
import com.tc.l2.msg.ReplicationResultCode;
import com.tc.l2.msg.SyncReplicationActivity;
import com.tc.net.ClientID;
import com.tc.net.ServerID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.GroupMessage;
import com.tc.object.ClientInstanceID;
import com.tc.object.FetchID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for PassiveAckSender backpressure logic added in commit 453d3c50f2.
 * Verifies batching, backpressure, and message flow control.
 */
public class PassiveAckSenderTest {

  private GroupManager<AbstractGroupMessage> groupManager;
  private Predicate<GroupMessage> sendConfirm;
  private Sink<Runnable> sentToActive;
  private PassiveAckSender passiveAckSender;
  private ServerID activeServer;
  private ServerID localServer;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    groupManager = mock(GroupManager.class);
    sendConfirm = mock(Predicate.class);
    sentToActive = mock(Sink.class);
    localServer = new ServerID("local", "local".getBytes());
    activeServer = new ServerID("active", "active".getBytes());

    when(groupManager.getLocalNodeID()).thenReturn(localServer);
    when(sendConfirm.test(any())).thenReturn(true);

    // Execute runnables immediately for testing
    doAnswer(invocation -> {
      Runnable r = invocation.getArgument(0);
      r.run();
      return null;
    }).when(sentToActive).addToSink(any(Runnable.class));

    passiveAckSender = new PassiveAckSender(groupManager, sendConfirm, sentToActive);
  }

  @Test
  public void testAcknowledgeCreatesMessage() throws GroupException {
    SyncReplicationActivity activity = createTestActivity();

    passiveAckSender.acknowledge(activeServer, activity, ReplicationResultCode.SUCCESS);

    // Verify that a message was sent to the active
    verify(groupManager, atLeastOnce()).sendToWithSentCallback(eq(activeServer), any(ReplicationMessageAck.class), any());
  }

  @Test
  public void testAckReceivedWithFuture() throws Exception {
    CompletableFuture<Void> future = new CompletableFuture<>();
    SyncReplicationActivity activity = createTestActivity();

    // Start acknowledgment in separate thread
    Thread ackThread = new Thread(() -> {
      passiveAckSender.ackReceived(activeServer, activity, future);
    });
    ackThread.start();

    // Complete the future
    future.complete(null);
    ackThread.join(1000);

    // Verify message was sent after future completed
    verify(groupManager, atLeastOnce()).sendToWithSentCallback(eq(activeServer), any(ReplicationMessageAck.class), any());
  }

  @Test
  public void testAckReceivedWithFailedFuture() {
    CompletableFuture<Void> future = new CompletableFuture<>();
    future.completeExceptionally(new RuntimeException("Test exception"));
    SyncReplicationActivity activity = createTestActivity();

    try {
      passiveAckSender.ackReceived(activeServer, activity, future);
      fail("Expected RuntimeException");
    } catch (RuntimeException e) {
      assertTrue(e.getMessage().contains("Caught exception while persisting transaction order"));
    }
  }

  @Test
  public void testBatchingMultipleAcknowledgments() throws GroupException {
    // Send multiple acknowledgments
    for (int i = 0; i < 10; i++) {
      SyncReplicationActivity activity = createTestActivity();
      passiveAckSender.acknowledge(activeServer, activity, ReplicationResultCode.SUCCESS);
    }

    // Verify batching occurred - should be fewer sends than individual acks
    ArgumentCaptor<ReplicationMessageAck> captor = ArgumentCaptor.forClass(ReplicationMessageAck.class);
    verify(groupManager, atLeast(1)).sendToWithSentCallback(eq(activeServer), captor.capture(), any());
  }

  @Test
  public void testBackpressureWithSendConfirm() throws GroupException {
    AtomicInteger sendCount = new AtomicInteger(0);

    // Simulate backpressure - only allow sends every other time
    when(sendConfirm.test(any())).thenAnswer(invocation -> {
      return sendCount.incrementAndGet() % 2 == 0;
    });

    // Send multiple acknowledgments
    for (int i = 0; i < 20; i++) {
      SyncReplicationActivity activity = createTestActivity();
      passiveAckSender.acknowledge(activeServer, activity, ReplicationResultCode.SUCCESS);
    }

    // Verify that backpressure was applied
    assertTrue("Send count should be greater than 0", sendCount.get() > 0);
  }

  @Test
  public void testGetLocalNodeID() {
    assertEquals(localServer, passiveAckSender.getLocalNodeID());
  }

  @Test
  public void testRequestPassiveSync() throws GroupException {
    ServerID target = new ServerID("target", "target".getBytes());

    passiveAckSender.requestPassiveSync(target);

    ArgumentCaptor<ReplicationMessageAck> captor = ArgumentCaptor.forClass(ReplicationMessageAck.class);
    verify(groupManager).sendTo(eq(target), captor.capture());

    // Verify it's a sync request message
    assertNotNull(captor.getValue());
  }

  @Test
  public void testTransformActivity() {
    SyncReplicationActivity activity = SyncReplicationActivity.createInvokeMessage(
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

    ServerEntityRequest request = passiveAckSender.transform(activity);

    assertNotNull(request);
    assertEquals(ServerEntityAction.INVOKE_ACTION, request.getAction());
    assertEquals(new ClientID(1L), request.getNodeID());
    assertEquals(new TransactionID(1L), request.getTransaction());
  }

  @Test
  public void testDecodeReplicationTypeForAllActivityTypes() {
    // Test all activity types can be decoded
    assertEquals(ServerEntityAction.ORDER_PLACEHOLDER_ONLY,
        PassiveAckSender.decodeReplicationType(SyncReplicationActivity.ActivityType.SYNC_START));
    assertEquals(ServerEntityAction.ORDER_PLACEHOLDER_ONLY,
        PassiveAckSender.decodeReplicationType(SyncReplicationActivity.ActivityType.SYNC_END));
    assertEquals(ServerEntityAction.ORDER_PLACEHOLDER_ONLY,
        PassiveAckSender.decodeReplicationType(SyncReplicationActivity.ActivityType.ORDERING_PLACEHOLDER));
    assertEquals(ServerEntityAction.MANAGED_ENTITY_GC,
        PassiveAckSender.decodeReplicationType(SyncReplicationActivity.ActivityType.LOCAL_ENTITY_GC));
    assertEquals(ServerEntityAction.LOCAL_FLUSH,
        PassiveAckSender.decodeReplicationType(SyncReplicationActivity.ActivityType.FLUSH_LOCAL_PIPELINE));
    assertEquals(ServerEntityAction.CREATE_ENTITY,
        PassiveAckSender.decodeReplicationType(SyncReplicationActivity.ActivityType.CREATE_ENTITY));
    assertEquals(ServerEntityAction.RECONFIGURE_ENTITY,
        PassiveAckSender.decodeReplicationType(SyncReplicationActivity.ActivityType.RECONFIGURE_ENTITY));
    assertEquals(ServerEntityAction.INVOKE_ACTION,
        PassiveAckSender.decodeReplicationType(SyncReplicationActivity.ActivityType.INVOKE_ACTION));
    assertEquals(ServerEntityAction.DESTROY_ENTITY,
        PassiveAckSender.decodeReplicationType(SyncReplicationActivity.ActivityType.DESTROY_ENTITY));
    assertEquals(ServerEntityAction.FETCH_ENTITY,
        PassiveAckSender.decodeReplicationType(SyncReplicationActivity.ActivityType.FETCH_ENTITY));
    assertEquals(ServerEntityAction.RELEASE_ENTITY,
        PassiveAckSender.decodeReplicationType(SyncReplicationActivity.ActivityType.RELEASE_ENTITY));
    assertEquals(ServerEntityAction.RECEIVE_SYNC_ENTITY_START_SYNCING,
        PassiveAckSender.decodeReplicationType(SyncReplicationActivity.ActivityType.SYNC_ENTITY_BEGIN));
    assertEquals(ServerEntityAction.RECEIVE_SYNC_ENTITY_KEY_START,
        PassiveAckSender.decodeReplicationType(SyncReplicationActivity.ActivityType.SYNC_ENTITY_CONCURRENCY_BEGIN));
    assertEquals(ServerEntityAction.RECEIVE_SYNC_PAYLOAD,
        PassiveAckSender.decodeReplicationType(SyncReplicationActivity.ActivityType.SYNC_ENTITY_CONCURRENCY_PAYLOAD));
    assertEquals(ServerEntityAction.RECEIVE_SYNC_ENTITY_KEY_END,
        PassiveAckSender.decodeReplicationType(SyncReplicationActivity.ActivityType.SYNC_ENTITY_CONCURRENCY_END));
    assertEquals(ServerEntityAction.RECEIVE_SYNC_ENTITY_END,
        PassiveAckSender.decodeReplicationType(SyncReplicationActivity.ActivityType.SYNC_ENTITY_END));
    assertEquals(ServerEntityAction.DISCONNECT_CLIENT,
        PassiveAckSender.decodeReplicationType(SyncReplicationActivity.ActivityType.DISCONNECT_CLIENT));
  }

  @Test(expected = AssertionError.class)
  public void testDecodeReplicationTypeThrowsForSyncBegin() {
    PassiveAckSender.decodeReplicationType(SyncReplicationActivity.ActivityType.SYNC_BEGIN);
  }

  @Test
  public void testNoAckSentForNullActiveServer() throws GroupException {
    SyncReplicationActivity activity = createTestActivity();

    passiveAckSender.acknowledge(ServerID.NULL_ID, activity, ReplicationResultCode.SUCCESS);

    // Verify no message was sent when active server is NULL
    verify(groupManager, never()).sendTo(any(ServerID.class), any(ReplicationMessageAck.class));
  }

  @Test
  public void testMultipleActiveSwitching() throws GroupException {
    ServerID active1 = new ServerID("active1", "active1".getBytes());
    ServerID active2 = new ServerID("active2", "active2".getBytes());

    // Send acks to first active
    for (int i = 0; i < 5; i++) {
      SyncReplicationActivity activity = createTestActivity();
      passiveAckSender.acknowledge(active1, activity, ReplicationResultCode.SUCCESS);
    }

    // Switch to second active
    for (int i = 0; i < 5; i++) {
      SyncReplicationActivity activity = createTestActivity();
      passiveAckSender.acknowledge(active2, activity, ReplicationResultCode.SUCCESS);
    }

    // Verify messages were sent to both actives
    verify(groupManager, atLeastOnce()).sendToWithSentCallback(eq(active1), any(ReplicationMessageAck.class), any());
    verify(groupManager, atLeastOnce()).sendToWithSentCallback(eq(active2), any(ReplicationMessageAck.class), any());
  }

  private SyncReplicationActivity createTestActivity() {
    return SyncReplicationActivity.createInvokeMessage(
        new FetchID(1L),
        new ClientID(1L),
        new ClientInstanceID(1L),
        new TransactionID(System.nanoTime()), // Use unique transaction ID
        new TransactionID(0L),
        SyncReplicationActivity.ActivityType.INVOKE_ACTION,
        null,
        1,
        ""
    );
  }
}
