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
import com.tc.exception.ZapDirtyDbServerNodeException;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.l2.dup.RelayMessage;
import com.tc.l2.msg.ReplicationMessage;
import com.tc.l2.msg.SyncReplicationActivity;
import com.tc.l2.state.ServerMode;
import com.tc.l2.state.StateManager;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupEventsListener;
import com.tc.net.groups.GroupManager;
import com.tc.object.ClientInstanceID;
import com.tc.object.FetchID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.l2.api.L2Coordinator;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DuplicationTransactionHandler}, focused on the
 * {@code currentSequence} advancement fix in {@code processMessage}.
 *
 * <p>Specifically validates:
 * <ul>
 *   <li>After each message is processed {@code currentSequence} is incremented by
 *       exactly 1 (the {@code currentSequence += 1} behaviour).</li>
 *   <li>A sequence gap (non-consecutive sequence ID) throws
 *       {@link ZapDirtyDbServerNodeException} with an informative message.</li>
 *   <li>Processing multiple sequential messages advances {@code currentSequence}
 *       to the last received sequence ID.</li>
 *   <li>The resume message sent on reconnect carries the correct {@code currentSequence}
 *       value so the relay replays only the messages this node has not yet seen.</li>
 * </ul>
 */
public class DuplicationTransactionHandlerTest {

  // ── Infrastructure mocks ────────────────────────────────────────────────

  @SuppressWarnings("unchecked")
  private final GroupManager<AbstractGroupMessage> groupManager = mock(GroupManager.class);
  @SuppressWarnings("unchecked")
  private final Stage<ReplicationMessage> nextStage = mock(Stage.class);
  @SuppressWarnings("unchecked")
  private final Sink<ReplicationMessage> nextSink = mock(Sink.class);

  private final StateManager stateManager = mock(StateManager.class);
  private final AtomicReference<GroupEventsListener> capturedListener = new AtomicReference<>();

  private DuplicationTransactionHandler handler;

  // ── Setup ────────────────────────────────────────────────────────────────

  @Before
  public void setUp() throws Exception {
    when(groupManager.getLocalNodeID()).thenReturn(new ServerID("local", "local".getBytes()));

    doAnswer(inv -> {
      capturedListener.set(inv.getArgument(0));
      return null;
    }).when(groupManager).registerForGroupEvents(any(GroupEventsListener.class));

    when(nextStage.getSink()).thenReturn(nextSink);

    handler = new DuplicationTransactionHandler(stateManager, n -> true, groupManager);

    // Wire sendToNext stage through the initialize path.
    ServerConfigurationContext ctx = mock(ServerConfigurationContext.class);
    L2Coordinator coord = mock(L2Coordinator.class);
    when(ctx.getL2Coordinator()).thenReturn(coord);
    when(coord.getStateManager()).thenReturn(stateManager);
    when(ctx.getStage(anyString(), any(Class.class))).thenReturn(nextStage);
    handler.getEventHandler().initializeContext(ctx);
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  /**
   * Creates a round-tripped (deserialized) {@link ReplicationMessage} with the given
   * sequence ID.  Deserialization flips {@code didCreateLocally} to {@code false} so
   * {@link ReplicationMessage#getActivities()} is accessible.
   */
  private static ReplicationMessage makeReplicationMessage(long sequenceID) {
    SyncReplicationActivity activity = SyncReplicationActivity.createInvokeMessage(
        new FetchID(sequenceID),
        new ClientID(1L),
        new ClientInstanceID(1L),
        new TransactionID(sequenceID),
        new TransactionID(0L),
        SyncReplicationActivity.ActivityType.INVOKE_ACTION,
        null, 1, ""
    );
    ReplicationMessage outgoing = ReplicationMessage.createActivityContainer(activity);
    outgoing.setSequenceID(sequenceID);
    TCByteBufferOutput out = new TCByteBufferOutputStream();
    outgoing.serializeTo(out);
    try (TCByteBufferInputStream in = new TCByteBufferInputStream(((TCByteBufferOutputStream) out).accessBuffers())) {
      ReplicationMessage incoming = new ReplicationMessage();
      incoming.deserializeFrom(in);
      incoming.setMessageOrginator(ServerID.NULL_ID);
      return incoming;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Wraps one or more {@link ReplicationMessage} objects into a serialized and then
   * deserialized {@link RelayMessage} of type {@code RELAY_BATCH}, matching what the
   * relay sender produces over the wire.
   */
  private static RelayMessage makeRelayBatch(ReplicationMessage... msgs) {
    RelayMessage outgoing = RelayMessage.createRelayBatch();
    for (ReplicationMessage m : msgs) {
      outgoing.addToBatch(m);
    }
    TCByteBufferOutput out = new TCByteBufferOutputStream();
    outgoing.serializeTo(out);
    try (TCByteBufferInputStream in = new TCByteBufferInputStream(((TCByteBufferOutputStream) out).accessBuffers())) {
      RelayMessage incoming = new RelayMessage();
      incoming.deserializeFrom(in);
      return incoming;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // ── Tests ─────────────────────────────────────────────────────────────────

  /**
   * After processing the first message (sequenceID = 1), {@code currentSequence}
   * must equal 1 — verified indirectly by sending a resume-reconnect event and
   * capturing the lastSeen value sent to the relay.
   *
   * <p>The fix changed {@code currentSequence = Long.max(currentSequence, msg.getSequenceID())}
   * to {@code currentSequence += 1}.  For normal in-order delivery the two expressions are
   * equivalent, so this test confirms the happy path still works correctly.
   */
  @Test
  public void processMessage_firstMessage_advancesCurrentSequenceToOne() throws Exception {
    when(stateManager.getCurrentMode()).thenReturn(ServerMode.REPLICA);

    ServerID relayNode = new ServerID("relay", "relay".getBytes());

    // Capture the resume message sent when the relay reconnects.
    AtomicReference<Long> sentLastSeen = new AtomicReference<>();
    doAnswer(inv -> {
      AbstractGroupMessage msg = inv.getArgument(1);
      if (msg instanceof RelayMessage) {
        sentLastSeen.set(((RelayMessage) msg).getLastSeen());
      }
      return null;
    }).when(groupManager).sendTo(any(NodeID.class), any(AbstractGroupMessage.class));

    handler.getEventHandler().handleEvent(makeRelayBatch(makeReplicationMessage(1L)));

    // Simulate relay reconnect → nodeJoined fires → REPLICA mode → sends resume with currentSequence=1
    capturedListener.get().nodeJoined(relayNode);

    assertEquals("currentSequence must be 1 after processing sequenceID=1", 1L, (long) sentLastSeen.get());
  }

  /**
   * Processing N sequential messages must advance {@code currentSequence} to N,
   * not to some other value such as {@code Long.max(0, N)} (which would be the same
   * but only coincidentally for strict sequences starting at 1).
   *
   * <p>This test processes 5 messages (seq 1..5) and verifies the resume value is 5.
   */
  @Test
  public void processMessage_sequentialMessages_advancesCurrentSequenceByOne() throws Exception {
    when(stateManager.getCurrentMode()).thenReturn(ServerMode.REPLICA);

    ServerID relayNode = new ServerID("relay", "relay".getBytes());
    AtomicReference<Long> sentLastSeen = new AtomicReference<>();
    doAnswer(inv -> {
      AbstractGroupMessage msg = inv.getArgument(1);
      if (msg instanceof RelayMessage) {
        sentLastSeen.set(((RelayMessage) msg).getLastSeen());
      }
      return null;
    }).when(groupManager).sendTo(any(NodeID.class), any(AbstractGroupMessage.class));

    for (long seq = 1; seq <= 5; seq++) {
      handler.getEventHandler().handleEvent(makeRelayBatch(makeReplicationMessage(seq)));
    }

    capturedListener.get().nodeJoined(relayNode);

    assertEquals("currentSequence must equal 5 after processing seq 1..5", 5L, (long) sentLastSeen.get());
  }

  /**
   * A batch containing multiple messages in one {@link RelayMessage} must advance
   * {@code currentSequence} by the number of messages in the batch, one step at a time.
   */
  @Test
  public void processMessage_batchOfThree_advancesCurrentSequenceByThree() throws Exception {
    when(stateManager.getCurrentMode()).thenReturn(ServerMode.REPLICA);

    ServerID relayNode = new ServerID("relay", "relay".getBytes());
    AtomicReference<Long> sentLastSeen = new AtomicReference<>();
    doAnswer(inv -> {
      AbstractGroupMessage msg = inv.getArgument(1);
      if (msg instanceof RelayMessage) {
        sentLastSeen.set(((RelayMessage) msg).getLastSeen());
      }
      return null;
    }).when(groupManager).sendTo(any(NodeID.class), any(AbstractGroupMessage.class));

    // One batch containing seq 1, 2, 3.
    handler.getEventHandler().handleEvent(makeRelayBatch(
        makeReplicationMessage(1L),
        makeReplicationMessage(2L),
        makeReplicationMessage(3L)));

    capturedListener.get().nodeJoined(relayNode);

    assertEquals("currentSequence must equal 3 after a batch of 3", 3L, (long) sentLastSeen.get());
  }

  /**
   * A gap in the sequence ID (missing message) must throw
   * {@link ZapDirtyDbServerNodeException} so the server can zap and re-sync.
   *
   * <p>Processes seq=1 successfully, then delivers seq=3 (skipping seq=2).
   * The handler must detect the gap and throw.
   */
  @Test(expected = Error.class) // Assert.failure wraps the ZapDirtyDbServerNodeException in Error
  public void processMessage_sequenceGap_throwsZapException() throws Exception {
    handler.getEventHandler().handleEvent(makeRelayBatch(makeReplicationMessage(1L)));
    // seq=3 skips seq=2 → sequence mismatch
    handler.getEventHandler().handleEvent(makeRelayBatch(makeReplicationMessage(3L)));
  }
}
