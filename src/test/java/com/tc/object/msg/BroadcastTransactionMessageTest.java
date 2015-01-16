/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.dna.api.LogicalChangeResult;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.locks.LockID;
import com.tc.object.locks.ServerLockContext.State;
import com.tc.object.locks.StringLockID;
import com.tc.object.locks.ThreadID;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnType;
import com.tc.server.BasicServerEvent;
import com.tc.server.ServerEvent;

import org.junit.Before;
import org.junit.Test;

import static com.tc.server.ServerEventType.EVICT;
import static com.tc.server.ServerEventType.PUT;
import static com.tc.server.ServerEventType.REMOVE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class BroadcastTransactionMessageTest {

  private BroadcastTransactionMessageImpl msg;
  private TCByteBufferOutputStream out;
  private MessageChannel channel;

  @Before
  public void setUp() throws Exception {
    this.channel = mock(MessageChannel.class);
    this.out = new TCByteBufferOutputStream(4, 4096, false);
    this.msg = new BroadcastTransactionMessageImpl(new SessionID(0), mock(MessageMonitor.class), this.out, this.channel,
        TCMessageType.BROADCAST_TRANSACTION_MESSAGE);
  }

  @Test
  public void testBasics() throws Exception {
    List<DNA> changes = new LinkedList<DNA>();
    // / XXX: TODO: Add changes to test.

    ObjectStringSerializer serializer = new ObjectStringSerializerImpl();
    LockID[] lockIDs = { new StringLockID("1") };
    long cid = 10;
    TransactionID txID = new TransactionID(1);
    ClientID clientID = new ClientID(1);
    GlobalTransactionID gtx = new GlobalTransactionID(2);
    TxnType txnType = TxnType.NORMAL;
    GlobalTransactionID lowGlobalTransactionIDWatermark = new GlobalTransactionID(1);

    Collection<ClientServerExchangeLockContext> notified = new LinkedList<ClientServerExchangeLockContext>();
    for (int i = 0; i < 100; i++) {
      notified.add(new ClientServerExchangeLockContext(new StringLockID(String.valueOf(i + 1)),
          clientID, new ThreadID(i + 1), State.WAITER));
    }

    final Map<LogicalChangeID, LogicalChangeResult> logicalChangeResults = new HashMap<LogicalChangeID, LogicalChangeResult>();
    logicalChangeResults.put(new LogicalChangeID(1), LogicalChangeResult.SUCCESS);
    logicalChangeResults.put(new LogicalChangeID(2), LogicalChangeResult.FAILURE);

    final List<ServerEvent> events = Arrays.<ServerEvent>asList(new BasicServerEvent(EVICT, "key-1", "cache1"),
        new BasicServerEvent(PUT, "key-2", "cache3"), new BasicServerEvent(REMOVE, "key-3", "cache2"));

    this.msg.initialize(changes, serializer, lockIDs, cid, txID, clientID, gtx, txnType,
                        lowGlobalTransactionIDWatermark, notified, new HashMap<String, ObjectID>(),
        logicalChangeResults, events);
    this.msg.dehydrate();

    TCByteBuffer[] data = this.out.toArray();
    TCMessageHeader header = (TCMessageHeader) this.msg.getHeader();
    this.msg = new BroadcastTransactionMessageImpl(SessionID.NULL_ID, mock(MessageMonitor.class), this.channel, header, data);
    this.msg.hydrate();

    assertEquals(changes, this.msg.getObjectChanges());
    assertEquals(Arrays.asList(lockIDs), this.msg.getLockIDs());
    assertEquals(cid, this.msg.getChangeID());
    assertEquals(txID, this.msg.getTransactionID());
    assertEquals(gtx, this.msg.getGlobalTransactionID());
    assertEquals(txnType, this.msg.getTransactionType());
    assertEquals(lowGlobalTransactionIDWatermark, this.msg.getLowGlobalTransactionIDWatermark());
    assertEquals(notified, this.msg.getNotifies());
    Map<LogicalChangeID, LogicalChangeResult> msgResults = this.msg.getLogicalChangeResults();
    assertEquals(2, msgResults.size());
    assertEquals(LogicalChangeResult.SUCCESS, msgResults.get(new LogicalChangeID(1)));
    assertEquals(LogicalChangeResult.FAILURE, msgResults.get(new LogicalChangeID(2)));
    assertEquals(events, this.msg.getEvents());
  }

}
