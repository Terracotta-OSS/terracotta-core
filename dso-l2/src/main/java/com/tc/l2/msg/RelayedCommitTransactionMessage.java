/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.async.api.OrderedEventContext;
import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.lang.Recyclable;
import com.tc.net.NodeID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.NodeIDSerializer;
import com.tc.net.protocol.tcm.TCMessageImpl;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class RelayedCommitTransactionMessage extends AbstractGroupMessage implements OrderedEventContext, Recyclable {

  public static final int        RELAYED_COMMIT_TXN_MSG_TYPE = 0;

  private TCByteBuffer[]         batchData;
  private ObjectStringSerializer serializer;
  private Map                    sid2gid;
  private NodeID                 nodeID;
  private long                   sequenceID;
  private TCMessageImpl          messageWrapper;

  /**
   * This message is recycled only at the read end (passive)
   */
  private final boolean          recyclable;

  private GlobalTransactionID    lowWaterMark;

  // To make serialization happy
  public RelayedCommitTransactionMessage() {
    super(-1);
    recyclable = true;
  }

  public RelayedCommitTransactionMessage(NodeID nodeID, TCByteBuffer[] batchData, ObjectStringSerializer serializer,
                                         Map sid2gid, long seqID, GlobalTransactionID lowWaterMark) {
    super(RELAYED_COMMIT_TXN_MSG_TYPE);
    this.nodeID = nodeID;
    this.batchData = batchData;
    this.serializer = serializer;
    this.sid2gid = sid2gid;
    this.sequenceID = seqID;
    this.lowWaterMark = lowWaterMark;
    this.recyclable = false;
  }

  public NodeID getClientID() {
    return nodeID;
  }

  public TCByteBuffer[] getBatchData() {
    return batchData;
  }

  public ObjectStringSerializer getSerializer() {
    return serializer;
  }

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    Assert.assertEquals(RELAYED_COMMIT_TXN_MSG_TYPE, getType());
    NodeIDSerializer nodeIDSerializer = new NodeIDSerializer();
    nodeIDSerializer = (NodeIDSerializer) nodeIDSerializer.deserializeFrom(in);
    this.nodeID = nodeIDSerializer.getNodeID();
    this.serializer = new ObjectStringSerializerImpl();
    this.serializer.deserializeFrom(in);
    this.sid2gid = readServerTxnIDGlobalTxnIDMapping(in);
    this.sequenceID = in.readLong();
    this.lowWaterMark = new GlobalTransactionID(in.readLong());
    int size = in.readInt();
    this.batchData = in.duplicateAndLimit(size).toArray();
  }

  @Override
  public boolean isRecycleOnRead(TCMessageImpl messages) {
    // delay recycling for reusing TCByteBuffers in batchData.
    this.messageWrapper = messages;
    return false;
  }

  private Map readServerTxnIDGlobalTxnIDMapping(TCByteBufferInput in) throws IOException {
    int size = in.readInt();
    Map mapping = new HashMap();
    NodeID cid = nodeID;
    for (int i = 0; i < size; i++) {
      TransactionID txnid = new TransactionID(in.readLong());
      GlobalTransactionID gid = new GlobalTransactionID(in.readLong());
      mapping.put(new ServerTransactionID(cid, txnid), gid);
    }
    return mapping;
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    Assert.assertEquals(RELAYED_COMMIT_TXN_MSG_TYPE, getType());
    NodeIDSerializer nodeIDSerializer = new NodeIDSerializer(nodeID);
    nodeIDSerializer.serializeTo(out);
    serializer.serializeTo(out);
    writeServerTxnIDGlobalTxnIDMapping(out);
    out.writeLong(this.sequenceID);
    out.writeLong(this.lowWaterMark.toLong());
    writeByteBuffers(out, this.batchData);
  }

  private void writeServerTxnIDGlobalTxnIDMapping(TCByteBufferOutput out) {
    out.writeInt(sid2gid.size());
    NodeID cid = nodeID;
    for (Iterator i = sid2gid.entrySet().iterator(); i.hasNext();) {
      Entry e = (Entry) i.next();
      ServerTransactionID sid = (ServerTransactionID) e.getKey();
      Assert.assertEquals(cid, sid.getSourceID());
      out.writeLong(sid.getClientTransactionID().toLong());
      GlobalTransactionID gid = (GlobalTransactionID) e.getValue();
      out.writeLong(gid.toLong());
    }
  }

  public GlobalTransactionID getGlobalTransactionIDFor(ServerTransactionID serverTransactionID) {
    GlobalTransactionID gid = (GlobalTransactionID) this.sid2gid.get(serverTransactionID);
    if (gid == null) { throw new AssertionError("No Mapping found for : " + serverTransactionID); }
    return gid;
  }

  public GlobalTransactionID getLowGlobalTransactionIDWatermark() {
    return this.lowWaterMark;
  }

  public long getSequenceID() {
    return sequenceID;
  }

  /**
   * Delayed message recycle here only at the read end (passive). this.batchData due to this.batchData reuses
   * TCByteBuffer from comm.
   */
  public void recycle() {
    Assert.assertTrue(recyclable);
    messageWrapper.recycle();
  }
}
