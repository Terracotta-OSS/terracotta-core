/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.l2.msg;

import com.tc.async.api.OrderedEventContext;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.MessageID;
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;
import java.io.IOException;

/**
 *
 */
public class ReplicationMessage extends AbstractGroupMessage implements OrderedEventContext {
//  message types  
  public static final int REPLICATE               = 0; // Sent to replicate a request on the passive
  public static final int SYNC               = 1; // Sent to replicate a request on the passive
  public static final int RESPONSE                = 2; // response that the replicated action completed
  public static final int START                = 3; // response that the replicated action completed

  public enum ReplicationType {
    NOOP,
    CREATE_ENTITY,
    INVOKE_ACTION,
    RELEASE_ENTITY,
    DESTROY_ENTITY,
    
    SYNC_BEGIN,
    SYNC_END,
    SYNC_ENTITY_BEGIN,
    SYNC_ENTITY_END,
    SYNC_ENTITY_CONCURRENCY_BEGIN,
    SYNC_ENTITY_CONCURRENCY_PAYLOAD,
    SYNC_ENTITY_CONCURRENCY_END;
  }
  
  EntityDescriptor descriptor;
    
  NodeID src;
  TransactionID tid;
  TransactionID oldest;

  ReplicationType action;
  byte[] payload;
  int concurrency;
  
  long rid;
  
  public ReplicationMessage() {
    super(REPLICATE);
  }
  
  public ReplicationMessage(int type) {
    super(type);
  }
  
  public ReplicationMessage(MessageID mid) {
    super(RESPONSE, mid);
  }  
//  a true replicated message
  public ReplicationMessage(EntityDescriptor descriptor, NodeID src, 
      TransactionID tid, TransactionID oldest, 
      ReplicationType action, byte[] payload, int concurrency) {
    super(REPLICATE);
    initialize(descriptor, src, tid, oldest, action, payload, concurrency);
  }
  
  protected final void initialize(EntityDescriptor descriptor, NodeID src, 
      TransactionID tid, TransactionID oldest, 
      ReplicationType action, byte[] payload, int concurrency) {
    Assert.assertNotNull(tid);
    Assert.assertNotNull(oldest);
    Assert.assertNotNull(src);
    this.descriptor = descriptor;
    this.src = src;
    this.tid = tid;
    this.oldest = oldest;
    this.action = action;
    this.payload = payload;
    this.concurrency = concurrency;
  }
  
  public ReplicationEnvelope target(NodeID node) {
    return new ReplicationEnvelope(node, this);
  }
  
  public void setReplicationID(long rid) {
    this.rid = rid;
  }

  @Override
  public long getSequenceID() {
    return rid;
  }
  
  public long getVersion() {
    return descriptor.getClientSideVersion();
  }
  
  public final ReplicationType getReplicationType() {
    return action;
  }

  public byte[] getExtendedData() {
    return payload;
  }
  
  public NodeID getSource() {
    return src;
  }
  
  public TransactionID getTransactionID() {
    return tid;
  }
  
  public TransactionID getOldestTransactionOnClient() {
    return oldest;
  }

  public EntityDescriptor getEntityDescriptor() {
    return descriptor;
  }
  
  public int getConcurrency() {
    return this.concurrency;
  }
  
  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    if (getType() == START) {
// do nothing, just need the source
    } else if (getType() == REPLICATE || getType() == SYNC) {
      this.rid = in.readLong();
      this.descriptor = EntityDescriptor.readFrom(in);
      int type = in.read();
      if (type == NodeID.CLIENT_NODE_TYPE) {
        this.src = new ClientID().deserializeFrom(in);
      } else if (type == NodeID.SERVER_NODE_TYPE) {
        this.src = new ServerID().deserializeFrom(in);
      }
      this.tid = new TransactionID(in.readLong());
      this.oldest = new TransactionID(in.readLong());
      this.action = ReplicationType.values()[in.readInt()];
      int length = in.readInt();
      this.payload = new byte[length];
      in.readFully(this.payload);
      this.concurrency = in.readInt();
    } else {
      this.rid = in.readLong();
    }
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    if (getType() == START) {
// do nothing, just need the source
    } else if (getType() == RESPONSE) {
//  do nothing, just need the messageid
      out.writeLong(rid);
    } else {
      out.writeLong(rid);
      this.descriptor.serializeTo(out);
      out.write(this.src.getNodeType());
      this.src.serializeTo(out);
      out.writeLong(tid.toLong());
      out.writeLong(oldest.toLong());
      out.writeInt(this.action.ordinal());
      if (payload != null) {
        out.writeInt(payload.length);
        out.write(payload);
      } else {
        out.writeInt(0);
      }
      out.writeInt(concurrency);
    }
  }

  @Override
  public String toString() {
    return "ReplicationMessage{rid=" + rid + ", id=" + descriptor + ", src=" + src + ", tid=" + tid + ", oldest=" + oldest + ", action=" + action + '}';
  }
}
