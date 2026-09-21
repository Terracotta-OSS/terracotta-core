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
import com.tc.l2.state.ServerMode;
import com.tc.objectserver.core.api.ServerConfigurationContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;
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
    when(stateManager.getCurrentMode()).thenReturn(ServerMode.RELAY);
    boolean registered = handler.registerRelayConsumer(replicaNodeID);

    assertFalse("Should not register when there is no active node", registered);
  }

  @Test
  public void registerRelayConsumer_returnsTrue_andForwardsSubsequentMessages() throws Exception {
    when(stateManager.getCurrentMode()).thenReturn(ServerMode.RELAY);
    boolean registered = handler.registerRelayConsumer(replicaNodeID);
    assertTrue("Should register when active node is known", registered);

    // Send a message through the event handler.
    handler.getEventHandler().handleEvent(makeMessage(1L));

    // The relay sink must have been driven (batch flushed to groupManager).
    verify(groupManager).sendToWithSentCallback(eq(replicaNodeID), any(AbstractGroupMessage.class), any());
  }

  @Test
  public void registerRelayConsumer_returnsFalse_whenAlreadyRegistered() throws Exception {
    when(stateManager.getCurrentMode()).thenReturn(ServerMode.RELAY);
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
    when(stateManager.getCurrentMode()).thenReturn(ServerMode.RELAY);

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
    when(stateManager.getCurrentMode()).thenReturn(ServerMode.RELAY);
    handler.registerRelayConsumer(replicaNodeID);

    List<ReplicationMessage> list = LongStream.of(1, 2, 3).mapToObj(this::makeMessage).toList();
    // Send messages seq 1..3.
    for (ReplicationMessage m : list) {
      handler.getEventHandler().handleEvent(m);
    }

    // Disconnect.
    capturedListener.get().nodeLeft(replicaNodeID);

    // The relay sink Runnable capture: replay may trigger additional sends.
    // Capture ALL messages sent to the replica over the lifetime of the test.
    ArgumentCaptor<AbstractGroupMessage> msgCaptor =
        ArgumentCaptor.forClass(AbstractGroupMessage.class);

    when(stateManager.getCurrentMode()).thenReturn(ServerMode.RELAY_CONNECTED);
    // Resume claiming last seen was seq=1; expects seq 2 and 3 to be replayed.
    boolean resumed = handler.resumeRelayConsumer(replicaNodeID, list.get(0).getSequenceID());

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
    when(stateManager.getCurrentMode()).thenReturn(ServerMode.RELAY);

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
    when(stateManager.getCurrentMode()).thenReturn(ServerMode.RELAY);

    handler.registerRelayConsumer(replicaNodeID);

    List<Long> seqs = List.of(10L, 20L, 30L, 40L, 50L);
    List<ReplicationMessage> result = seqs.stream().map(this::makeMessage).toList();
    for (ReplicationMessage s : result) {
      handler.getEventHandler().handleEvent(s);
    }

    capturedListener.get().nodeLeft(replicaNodeID);
    when(stateManager.getCurrentMode()).thenReturn(ServerMode.RELAY_CONNECTED);
    // Resume from seq=20 → should replay 30, 40, 50
    boolean resumed = handler.resumeRelayConsumer(replicaNodeID, result.get(2).getSequenceID());
    assertTrue(resumed);

    // 5 original sends (10,20,30,40,50) + 1 replay flush (30,40,50 batched) = 6
    verify(groupManager, times(6)).sendToWithSentCallback(eq(replicaNodeID), any(), any());
  }

  // ── generationOffset tests ────────────────────────────────────────────────

  /**
   * The initial generationOffset is 1, so the first message with rid=N arrives and is
   * stamped with sequenceID = 1 + N.  Verify the stamped value by collecting messages
   * that arrive at the relay replica and reading their sequence IDs.
   */
  @Test
  public void generationOffset_initialValue_stampsMessagesWithOffsetPlusRid() throws Exception {
    when(stateManager.getCurrentMode()).thenReturn(ServerMode.RELAY);
    handler.registerRelayConsumer(replicaNodeID);

    // makeMessage(rid) builds a message whose pre-addToHistory sequenceID == rid.
    // After addToHistory the handler adds generationOffset (initial value = 1).
    ReplicationMessage msg = makeMessage(5L);
    handler.getEventHandler().handleEvent(msg);

    // addToHistory mutates the message in place: sequenceID becomes 1 + 5 = 6.
    assertEquals("Initial generationOffset=1 must produce sequenceID == rid + 1", 6L, msg.getSequenceID());
  }

  /**
   * After the active node disconnects ({@code nodeLeft(activeID)}), {@code clearHistory}
   * advances {@code generationOffset} to the maximum sequenceID seen in the current
   * generation.  Messages from the next active therefore get IDs strictly greater than
   * all IDs from the previous generation.
   *
   * <p>The relay server remains connected to the same downstream replica across the active
   * failover; new active messages arrive while the forward batch context is still live.
   */
  @Test
  public void generationOffset_advancesAfterActiveLeavesAndClearsHistory() throws Exception {
    when(stateManager.getCurrentMode()).thenReturn(ServerMode.RELAY);
    handler.registerRelayConsumer(replicaNodeID);

    // Generation 1: messages rid=1,2,3 → stamped as 1+1=2, 1+2=3, 1+3=4 (initial offset=1).
    List<ReplicationMessage> gen1 = new ArrayList<>();
    for (long rid = 1; rid <= 3; rid++) {
      ReplicationMessage m = makeMessage(rid);
      handler.getEventHandler().handleEvent(m);
      gen1.add(m);
    }
    long maxGen1SeqID = gen1.stream().mapToLong(ReplicationMessage::getSequenceID).max().getAsLong();
    // maxGen1SeqID == 4 (offset 1 + rid 3)

    // Active leaves — clearHistory() fires, advancing generationOffset to maxGen1SeqID (4).
    capturedListener.get().nodeLeft(activeNodeID);

    // New active takes over; relay remains connected to the same downstream.
    // A new message from the replacement active starts its rids from 1 again.
    ReplicationMessage gen2msg = makeMessage(1L);
    handler.getEventHandler().handleEvent(gen2msg);

    // New offset = 4, so stamped sequenceID = 4 + 1 = 5 > maxGen1SeqID (4).
    assertTrue(
        "Generation-2 message sequenceID must exceed the max of generation 1",
        gen2msg.getSequenceID() == maxGen1SeqID + 1
    );
  }

  /**
   * After an active failover the relay server's generationOffset is the highest stamped
   * sequence ID from the previous generation.  The first message of the new generation
   * (whose rid starts at 1 again) therefore receives an ID strictly greater than every
   * ID from the previous generation — guaranteeing monotonically increasing sequence IDs
   * as seen by the downstream replica.
   */
  @Test
  public void generationOffset_newGenerationIDsDoNotOverlapPreviousGeneration() throws Exception {
    when(stateManager.getCurrentMode()).thenReturn(ServerMode.RELAY);
    handler.registerRelayConsumer(replicaNodeID);

    // Generation 1: rids 1..5, stamped as offset(1)+rid = 2,3,4,5,6.
    List<Long> gen1StampedIDs = new ArrayList<>();
    for (long rid = 1; rid <= 5; rid++) {
      ReplicationMessage m = makeMessage(rid);
      handler.getEventHandler().handleEvent(m);
      gen1StampedIDs.add(m.getSequenceID());
    }
    long maxGen1 = gen1StampedIDs.stream().mapToLong(Long::longValue).max().getAsLong();
    // maxGen1 == 6

    // Active 1 leaves → clearHistory sets generationOffset = 6.
    capturedListener.get().nodeLeft(activeNodeID);

    // Generation 2: new active restarts its own rid sequence from 1.
    // Expected stamped IDs: 6+1=7, 6+2=8, 6+3=9, 6+4=10, 6+5=11.
    List<Long> gen2StampedIDs = new ArrayList<>();
    for (long rid = 1; rid <= 5; rid++) {
      ReplicationMessage m = makeMessage(rid);
      handler.getEventHandler().handleEvent(m);
      gen2StampedIDs.add(m.getSequenceID());
    }
    long minGen2 = gen2StampedIDs.stream().mapToLong(Long::longValue).min().getAsLong();

    assertTrue(
        "Every gen-2 stamped ID must be strictly greater than the max gen-1 ID",
        minGen2 > maxGen1
    );
  }

  /**
   * {@code clearHistory} with an empty ring buffer (no messages ever processed) must not
   * crash and must leave generationOffset at the initial value of 1, so the first message
   * of the next generation is still stamped as 1 + rid.
   *
   * <p>The active node must first be tracked (via {@code registerRelayConsumer} which calls
   * {@code replayHistory} and sets {@code activeID}) before a {@code nodeLeft(activeID)}
   * can trigger {@code clearHistory}.
   */
  @Test
  public void generationOffset_clearHistoryOnEmptyBuffer_keepsInitialOffset() throws Exception {
    // Register a consumer so that activeID is set inside replayHistory.
    when(stateManager.getCurrentMode()).thenReturn(ServerMode.RELAY);
    handler.registerRelayConsumer(replicaNodeID);

    // No messages sent yet — history is empty.
    // Active leaves: clearHistory fires but history.stream().max() is empty → orElse(1L) keeps offset at 1.
    capturedListener.get().nodeLeft(activeNodeID);

    // A message from the new active with rid=10 must still be stamped as 1 + 10 = 11.
    ReplicationMessage msg = makeMessage(10L);
    handler.getEventHandler().handleEvent(msg);

    assertEquals("After empty-buffer clearHistory the offset must remain 1",
        11L, msg.getSequenceID());
  }
}
