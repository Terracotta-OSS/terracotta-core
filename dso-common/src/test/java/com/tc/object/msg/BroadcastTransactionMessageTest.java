/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.TestMessageChannel;
import com.tc.object.dmi.DmiDescriptor;
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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

public class BroadcastTransactionMessageTest extends TestCase {

  private BroadcastTransactionMessageImpl msg;
  private TCByteBufferOutputStream        out;
  private MessageMonitor                  monitor;
  private MessageChannel                  channel;

  @Override
  public void setUp() throws Exception {
    this.monitor = new NullMessageMonitor();
    this.channel = new TestMessageChannel();
    this.out = new TCByteBufferOutputStream(4, 4096, false);
    this.msg = new BroadcastTransactionMessageImpl(new SessionID(0), this.monitor, this.out, this.channel,
                                                   TCMessageType.BROADCAST_TRANSACTION_MESSAGE);
  }

  public void testBasics() throws Exception {
    List changes = new LinkedList();
    // / XXX: TODO: Add changes to test.

    ObjectStringSerializer serializer = new ObjectStringSerializerImpl();
    LockID[] lockIDs = new LockID[] { new StringLockID("1") };
    long cid = 10;
    TransactionID txID = new TransactionID(1);
    ClientID clientID = new ClientID(1);
    GlobalTransactionID gtx = new GlobalTransactionID(2);
    TxnType txnType = TxnType.NORMAL;
    GlobalTransactionID lowGlobalTransactionIDWatermark = new GlobalTransactionID(1);

    Collection notified = new LinkedList();
    for (int i = 0; i < 100; i++) {
      notified.add(new ClientServerExchangeLockContext(new StringLockID("" + (i + 1)), clientID, new ThreadID(i + 1),
                                                       State.WAITER));
    }
    this.msg.initialize(changes, serializer, lockIDs, cid, txID, clientID, gtx, txnType,
                        lowGlobalTransactionIDWatermark, notified, new HashMap(), DmiDescriptor.EMPTY_ARRAY);
    this.msg.dehydrate();

    TCByteBuffer[] data = this.out.toArray();
    TCMessageHeader header = (TCMessageHeader) this.msg.getHeader();
    this.msg = new BroadcastTransactionMessageImpl(SessionID.NULL_ID, this.monitor, this.channel, header, data);
    this.msg.hydrate();

    assertEquals(changes, this.msg.getObjectChanges());
    assertEquals(Arrays.asList(lockIDs), this.msg.getLockIDs());
    assertEquals(cid, this.msg.getChangeID());
    assertEquals(txID, this.msg.getTransactionID());
    assertEquals(gtx, this.msg.getGlobalTransactionID());
    assertEquals(txnType, this.msg.getTransactionType());
    assertEquals(lowGlobalTransactionIDWatermark, this.msg.getLowGlobalTransactionIDWatermark());
    assertEquals(notified, this.msg.addNotifiesTo(new LinkedList()));
  }

}
