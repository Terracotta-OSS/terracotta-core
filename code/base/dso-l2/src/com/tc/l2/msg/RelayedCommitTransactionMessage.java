/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.async.api.OrderedEventContext;
import com.tc.bytes.TCByteBuffer;
import com.tc.lang.Recyclable;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.NodeID;
import com.tc.net.groups.NodeIDSerializer;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.gtx.GlobalTransactionIDGenerator;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class RelayedCommitTransactionMessage extends AbstractGroupMessage implements OrderedEventContext,
    GlobalTransactionIDGenerator, Recyclable {

  public static final int        RELAYED_COMMIT_TXN_MSG_TYPE = 0;

  private TCByteBuffer[]         batchData;
  private ObjectStringSerializer serializer;
  private Map                    sid2gid;
  private NodeID                 nodeID;
  private long                   sequenceID;

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

  protected void basicReadExternal(int msgType, ObjectInput in) throws IOException, ClassNotFoundException {
    Assert.assertEquals(RELAYED_COMMIT_TXN_MSG_TYPE, msgType);
    this.nodeID = NodeIDSerializer.readNodeID(in);
    this.serializer = readObjectStringSerializer(in);
    this.batchData = readByteBuffers(in);
    this.sid2gid = readServerTxnIDglobalTxnIDMapping(in);
    this.sequenceID = in.readLong();
    this.lowWaterMark = new GlobalTransactionID(in.readLong());
  }

  private Map readServerTxnIDglobalTxnIDMapping(ObjectInput in) throws IOException {
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

  protected void basicWriteExternal(int msgType, ObjectOutput out) throws IOException {
    Assert.assertEquals(RELAYED_COMMIT_TXN_MSG_TYPE, msgType);
    NodeIDSerializer.writeNodeID(nodeID, out);
    writeObjectStringSerializer(out, serializer);
    writeByteBuffers(out, batchData);
    writeServerTxnIDGlobalTxnIDMapping(out);
    out.writeLong(this.sequenceID);
    out.writeLong(this.lowWaterMark.toLong());
  }

  private void writeServerTxnIDGlobalTxnIDMapping(ObjectOutput out) throws IOException {
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

  public GlobalTransactionID getOrCreateGlobalTransactionID(ServerTransactionID serverTransactionID) {
    GlobalTransactionID gid = (GlobalTransactionID) this.sid2gid.get(serverTransactionID);
    if (gid == null) { throw new AssertionError("no Mapping found for : " + serverTransactionID); }
    return gid;
  }

  public GlobalTransactionID getLowGlobalTransactionIDWatermark() {
    return this.lowWaterMark;
  }

  public long getSequenceID() {
    return sequenceID;
  }

  /**
   * This message is recycled only at the read end (passive)
   */
  public void recycle() {
    Assert.assertTrue(recyclable);
    for (int i = 0; i < batchData.length; i++) {
      batchData[i].recycle();
    }
  }

}
