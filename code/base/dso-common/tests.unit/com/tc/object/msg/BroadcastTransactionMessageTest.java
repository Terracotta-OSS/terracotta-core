/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.TestMessageChannel;
import com.tc.object.ObjectID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.LockRequest;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnType;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

public class BroadcastTransactionMessageTest extends TestCase {

  private BroadcastTransactionMessageImpl msg;
  private TCByteBufferOutput              out;
  private MessageMonitor                  monitor;
  private MessageChannel                  channel;

  public void setUp() throws Exception {
    monitor = new NullMessageMonitor();
    channel = new TestMessageChannel();
    out = new TCByteBufferOutputStream(4, 4096, false);
    msg = new BroadcastTransactionMessageImpl(monitor, out, channel, TCMessageType.BROADCAST_TRANSACTION_MESSAGE);
  }

  public void testBasics() throws Exception {
    List changes = new LinkedList();
    // / XXX: TODO: Add changes to test.

    ObjectStringSerializer serializer = new ObjectStringSerializer();
    LockID[] lockIDs = new LockID[] { new LockID("1") };
    long cid = 10;
    TransactionID txID = new TransactionID(1);
    ChannelID channelID = new ChannelID(1);
    GlobalTransactionID gtx = new GlobalTransactionID(2);
    TxnType txnType = TxnType.NORMAL;
    GlobalTransactionID lowGlobalTransactionIDWatermark = new GlobalTransactionID(1);

    Collection notified = new LinkedList();
    Set lookupObjectIDs = new HashSet();
    for (int i = 0; i < 100; i++) {
      notified.add(new LockRequest(new LockID("" + (i + 1)), new ThreadID(i + 1), LockLevel.WRITE));
      lookupObjectIDs.add(new ObjectID(i));
    }
    msg.initialize(changes, lookupObjectIDs, serializer, lockIDs, cid, txID, channelID, gtx, txnType,
                   lowGlobalTransactionIDWatermark, notified, new HashMap(), DmiDescriptor.EMPTY_ARRAY);
    msg.dehydrate();

    TCByteBuffer[] data = out.toArray();
    TCMessageHeader header = (TCMessageHeader) msg.getHeader();
    msg = new BroadcastTransactionMessageImpl(SessionID.NULL_ID, monitor, channel, header, data);
    msg.hydrate();

    assertEquals(changes, msg.getObjectChanges());
    assertEquals(Arrays.asList(lockIDs), Arrays.asList(msg.getLockIDs()));
    assertEquals(cid, msg.getChangeID());
    assertEquals(txID, msg.getTransactionID());
    assertEquals(gtx, msg.getGlobalTransactionID());
    assertEquals(txnType, msg.getTransactionType());
    assertEquals(lowGlobalTransactionIDWatermark, msg.getLowGlobalTransactionIDWatermark());
    assertEquals(notified, msg.addNotifiesTo(new LinkedList()));
    assertEquals(lookupObjectIDs, msg.getLookupObjectIDs());
  }

}
