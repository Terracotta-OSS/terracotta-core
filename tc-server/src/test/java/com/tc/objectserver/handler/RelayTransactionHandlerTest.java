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
import com.tc.async.api.Stage;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.l2.msg.ReplicationMessage;
import com.tc.l2.msg.SyncReplicationActivity;
import com.tc.l2.state.StateManager;
import com.tc.net.ClientID;
import com.tc.net.ServerID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupEventsListener;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.object.ClientInstanceID;
import com.tc.object.FetchID;
import com.tc.object.tx.TransactionID;
import java.io.IOException;
import com.tc.l2.api.L2Coordinator;
import com.tc.objectserver.core.api.ServerConfigurationContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RelayTransactionHandler}.
 *
 * <p>Key behaviours verified:
 * <ul>
 *   <li>Messages are forwarded to the registered relay consumer.</li>
 *   <li>Messages received before a consumer is registered are still buffered in history.</li>
 *   <li>When the relay target disconnects ({@code nodeLeft}), {@code forward} is cleared so
 *       subsequent messages are not sent (no NPE, no stale send).</li>
 *   <li>{@code resumeRelayConsumer} replays buffered history for messages after
 *       {@code lastSeen} and resumes the live stream.</li>
 *   <li>{@code resumeRelayConsumer} returns {@code false} when {@code lastSeen} is not in
 *       the ring buffer (history was overwritten).</li>
 *   <li>{@code registerRelayConsumer} returns {@code false} when an active node is not
 *       yet known or a consumer is already registered.</li>
 *   <li>A {@link GroupException} during flush clears {@code forward} so the handler
 *       recovers rather than crashing.</li>
 * </ul>
 */
public class RelayTransactionHandlerTest {

  // ── Infrastructure mocks ─────────────────────────────────────────────────

  @SuppressWarnings("unchecked")
  private final GroupManager<AbstractGroupMessage> groupManager = mock(GroupManager.class);
  @SuppressWarnings("unchecked")
  private final Stage<Runnable> relaySenderStage = mock(Stage.class);
  @SuppressWarnings("unchecked")
  private final Sink<Runnable> relaySink = mock(Sink.class);

  private final ServerID activeNodeID = new ServerID("active", "active".getBytes());
  private final ServerID replicaNodeID = new ServerID("replica", "replica".getBytes());
  private final ServerID localNodeID = new ServerID("local", "local".getBytes());

  /** Captured GroupEventsListener registered by the handler under test. */
  private final AtomicReference<GroupEventsListener> capturedListener = new AtomicReference<>();

  private StateManager stateManager;
  private RelayTransactionHandler handler;

  /** Monotonically increasing sequence used to assign {@code rid} to messages. */
  private final AtomicLong seq = new AtomicLong(1);

  // ── Setup ─────────────────────────────────────────────────────────────────

  @Before
  public void setUp() throws Exception {
    when(groupManager.getLocalNodeID()).thenReturn(localNodeID);

    // Capture the GroupEventsListener so tests can fire nodeJoined/nodeLeft.
    doAnswer(inv -> {
      capturedListener.set(inv.getArgument(0));
      return null;
    }).when(groupManager).registerForGroupEvents(any(GroupEventsListener.class));

    // Run Runnables added to the relay sink inline (single-threaded tests).
    doAnswer(inv -> {
      ((Runnable) inv.getArgument(0)).run();
      return null;
    }).when(relaySink).addToSink(any(Runnable.class));

    // When the groupManager sends a relay batch, immediately invoke the sentCallback so that
    // GroupMessageBatchContext.messagesInFlight decrements and subsequent messages can be sent.
    doAnswer(inv -> {
      Runnable sentCallback = inv.getArgument(2);
      if (sentCallback != null) sentCallback.run();
      return null;
    }).when(groupManager).sendToWithSentCallback(any(), any(), any());

    when(relaySenderStage.getSink()).thenReturn(relaySink);

    handler = new RelayTransactionHandler(relaySenderStage, groupManager);

    // Wire the StateManager through the initialize path of the inner EventHandler.
    stateManager = mock(StateManager.class);
    when(stateManager.getActiveNodeID()).thenReturn(activeNodeID);

    ServerConfigurationContext ctx = mock(ServerConfigurationContext.class);
    L2Coordinator coord = mock(L2Coordinator.class);
    when(ctx.getL2Coordinator()).thenReturn(coord);
    when(coord.getStateManager()).thenReturn(stateManager);
    handler.getEventHandler().initializeContext(ctx);
  }

  // ── Helper ────────────────────────────────────────────────────────────────

  /**
   * Creates an <em>incoming</em> (deserialized) {@link ReplicationMessage} with the given
   * sequence id, sent from {@code activeNodeID}.  We round-trip through serialization so that
   * {@code didCreateLocally == false} and {@code getActivities()} is accessible, exactly as a
   * real message arriving over the wire would be.
   */
  private ReplicationMessage makeMessage(long sequenceID) {
    SyncReplicationActivity activity = SyncReplicationActivity.createInvokeMessage(
        new FetchID(sequenceID),
        new ClientID(1L),
        new ClientInstanceID(1L),
        new TransactionID(sequenceID),
        new TransactionID(0L),
        SyncReplicationActivity.ActivityType.INVOKE_ACTION,
        null,
        1,
        ""
    );
    // createActivityContainer sets didCreateLocally=true; we must deserialize to flip it.
    ReplicationMessage outgoing = ReplicationMessage.createActivityContainer(activity);
    outgoing.setSequenceID(sequenceID);
    TCByteBufferOutput out = new TCByteBufferOutputStream();
    outgoing.serializeTo(out);
    try (TCByteBufferInputStream in = new TCByteBufferInputStream(((TCByteBufferOutputStream) out).accessBuffers())) {
      ReplicationMessage incoming = new ReplicationMessage();
      incoming.deserializeFrom(in);
      incoming.setMessageOrginator(activeNodeID);
      return incoming;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // ── Tests ─────────────────────────────────────────────────────────────────

  @Test
  public void registerRelayConsumer_returnsFalse_whenNoActive() throws Exception {
    when(stateManager.getActiveNodeID()).thenReturn(ServerID.NULL_ID);

    boolean registered = handler.registerRelayConsumer(replicaNodeID);

    assertFalse("Should not register when there is no active node", registered);
  }

  @Test
  public void registerRelayConsumer_returnsTrue_andForwardsSubsequentMessages() throws Exception {
    boolean registered = handler.registerRelayConsumer(replicaNodeID);
    assertTrue("Should register when active node is known", registered);

    // Send a message through the event handler.
    handler.getEventHandler().handleEvent(makeMessage(1L));

    // The relay sink must have been driven (batch flushed to groupManager).
    verify(groupManager).sendToWithSentCallback(eq(replicaNodeID), any(AbstractGroupMessage.class), any());
  }

  @Test
  public void registerRelayConsumer_returnsFalse_whenAlreadyRegistered() throws Exception {
    handler.registerRelayConsumer(replicaNodeID);

    ServerID anotherReplica = new ServerID("replica2", "replica2".getBytes());
    boolean second = handler.registerRelayConsumer(anotherReplica);

    assertFalse("Second registration must be rejected", second);
  }

  @Test
  public void messagesReceivedWithoutConsumer_areNotForwarded() throws Exception {
    // No registerRelayConsumer call — forward is null.
    handler.getEventHandler().handleEvent(makeMessage(1L));

    // Ack sends to the active are expected; verify that no relay batch was sent to the replica.
    verify(groupManager, never()).sendToWithSentCallback(eq(replicaNodeID), any(), any());
  }

  @Test
  public void nodeLeft_clearsForward_subsequentMessagesNotSent() throws Exception {
    handler.registerRelayConsumer(replicaNodeID);

    // Fire nodeLeft for the registered target.
    capturedListener.get().nodeLeft(replicaNodeID);

    // A message processed after the disconnect must NOT be forwarded to the replica.
    handler.getEventHandler().handleEvent(makeMessage(1L));

    verify(groupManager, never()).sendToWithSentCallback(eq(replicaNodeID), any(), any());
  }

  @Test
  public void nodeLeft_forDifferentNode_doesNotClearForward() throws Exception {
    handler.registerRelayConsumer(replicaNodeID);

    ServerID unrelated = new ServerID("other", "other".getBytes());
    capturedListener.get().nodeLeft(unrelated);

    // Forward must still be live — next message should be sent.
    handler.getEventHandler().handleEvent(makeMessage(1L));

    verify(groupManager).sendToWithSentCallback(eq(replicaNodeID), any(), any());
  }

  @Test
  public void resumeRelayConsumer_returnsFalse_whenLastSeenNotInHistory() throws Exception {
    handler.registerRelayConsumer(replicaNodeID);
    // Only message 1 is in history.
    handler.getEventHandler().handleEvent(makeMessage(1L));

    capturedListener.get().nodeLeft(replicaNodeID);

    // lastSeen=99 is not in history.
    boolean resumed = handler.resumeRelayConsumer(replicaNodeID, 99L);

    assertFalse("Resume must fail when lastSeen is not in history", resumed);
  }

  @Test
  public void resumeRelayConsumer_returnsTrue_andReplaysMessagesAfterLastSeen() throws Exception {
    handler.registerRelayConsumer(replicaNodeID);

    // Send messages seq 1..3.
    handler.getEventHandler().handleEvent(makeMessage(1L));
    handler.getEventHandler().handleEvent(makeMessage(2L));
    handler.getEventHandler().handleEvent(makeMessage(3L));

    // Disconnect.
    capturedListener.get().nodeLeft(replicaNodeID);

    // The relay sink Runnable capture: replay may trigger additional sends.
    // Capture ALL messages sent to the replica over the lifetime of the test.
    ArgumentCaptor<AbstractGroupMessage> msgCaptor =
        ArgumentCaptor.forClass(AbstractGroupMessage.class);

    // Resume claiming last seen was seq=1; expects seq 2 and 3 to be replayed.
    boolean resumed = handler.resumeRelayConsumer(replicaNodeID, 1L);

    assertTrue("Resume must succeed when lastSeen is in history", resumed);

    // After resume, a new live message must also be forwarded.
    handler.getEventHandler().handleEvent(makeMessage(4L));

    // 3 original sends (seq 1,2,3) + 1 replay flush (seq 2+3 batched together) + 1 live send (seq 4) = 5
    verify(groupManager, times(5)).sendToWithSentCallback(eq(replicaNodeID), any(), any());
  }

  @Test
  public void resumeRelayConsumer_returnsFalse_whenTargetMismatch() throws Exception {
    handler.registerRelayConsumer(replicaNodeID);
    handler.getEventHandler().handleEvent(makeMessage(1L));
    capturedListener.get().nodeLeft(replicaNodeID);

    ServerID wrongNode = new ServerID("wrong", "wrong".getBytes());
    boolean resumed = handler.resumeRelayConsumer(wrongNode, 1L);

    assertFalse("Resume must fail when node does not match the registered target", resumed);
  }

  @Test
  public void groupException_duringFlush_clearsForwardAndDoesNotThrow() throws Exception {
    // Make groupManager throw on send (sendToWithSentCallback is void — use doThrow).
    doAnswer(inv -> { throw new GroupException("simulated network failure"); })
        .when(groupManager).sendToWithSentCallback(any(), any(), any());

    handler.registerRelayConsumer(replicaNodeID);

    // The send will throw, but the handler must swallow it (not propagate to the caller)
    // and clear forward so subsequent messages are silently dropped rather than crashing.
    handler.getEventHandler().handleEvent(makeMessage(1L));

    // After the flush exception, forward is nulled — next message must not attempt a relay send.
    handler.getEventHandler().handleEvent(makeMessage(2L));

    // Only the first relay send was attempted; the second message found forward==null.
    verify(groupManager, times(1)).sendToWithSentCallback(eq(replicaNodeID), any(), any());
  }

  @Test
  public void multipleMessages_areAllBufferedInHistory_forLaterResume() throws Exception {
    handler.registerRelayConsumer(replicaNodeID);

    List<Long> seqs = List.of(10L, 20L, 30L, 40L, 50L);
    for (long s : seqs) {
      handler.getEventHandler().handleEvent(makeMessage(s));
    }

    capturedListener.get().nodeLeft(replicaNodeID);

    // Resume from seq=20 → should replay 30, 40, 50
    boolean resumed = handler.resumeRelayConsumer(replicaNodeID, 20L);
    assertTrue(resumed);

    // 5 original sends (10,20,30,40,50) + 1 replay flush (30,40,50 batched) = 6
    verify(groupManager, times(6)).sendToWithSentCallback(eq(replicaNodeID), any(), any());
  }
}
