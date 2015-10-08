/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.async.api.OrderedEventContext;
import com.tc.entity.VoltronEntityMessage;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.MessageID;
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;
import java.io.IOException;

/**
 *
 */
public class ReplicationMessage extends AbstractGroupMessage implements VoltronEntityMessage, OrderedEventContext {
//  message types  
  public static final int REPLICATE               = 0; // Sent to replicate a request on the passive
  public static final int RESPONSE                = 1; // response that the replicated action completed
  
  public static final int NOOP = 0;
  public static final int CREATE_ENTITY = 1;
  public static final int INVOKE_ACTION = 2;
  public static final int GET_ENTITY = 3;
  public static final int RELEASE_ENTITY = 4;
  public static final int DESTROY_ENTITY = 5;
  public static final int PROMOTE_ENTITY_TO_ACTIVE = 6;
  public static final int SYNC_ENTITY = 7;  
  
  EntityDescriptor descriptor;
  long version;
    
  NodeID src;
  Iterable<NodeID> destination;
  TransactionID tid;
  TransactionID oldest;

  int action;
  byte[] payload;
  
  long rid;
  
  public ReplicationMessage() {
    super(REPLICATE);
  }
  
  public ReplicationMessage(MessageID mid) {
    super(RESPONSE, mid);
  }  
  
  public ReplicationMessage(EntityDescriptor descriptor, long version, NodeID src, 
      Iterable<NodeID> dest, TransactionID tid, TransactionID oldest, 
      int action, byte[] payload, long rid) {
    super(REPLICATE);
    this.descriptor = descriptor;
    this.version = version;
    this.src = src;
    this.destination = dest;
    this.tid = tid;
    this.oldest = oldest;
    this.action = action;
    this.payload = payload;
    this.rid = rid;
  }

  @Override
  public long getSequenceID() {
    return rid;
  }
  
  public long getVersion() {
    return version;
  }

  @Override
  public byte[] getExtendedData() {
    return payload;
  }
  
  public int getAction() {
    return action;
  }
  
  @Override
  public NodeID getSource() {
    return src;
  }
  
  @Override
  public TransactionID getTransactionID() {
    return tid;
  }
  
  @Override
  public TransactionID getOldestTransactionOnClient() {
    return oldest;
  }
  
  public Iterable<NodeID> getDestinations() {
    return destination;
  }

  @Override
  public boolean doesRequireReplication() {
    return false;
  }

  @Override
  public EntityDescriptor getEntityDescriptor() {
    return descriptor;
  }

  @Override
  public VoltronEntityMessage.Type getVoltronType() {
    return decodeServerActionType(action);
  }
  
  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    if (getType() == REPLICATE) {
      this.rid = in.readLong();
      this.descriptor = EntityDescriptor.readFrom(in);
      this.version = in.readLong();
      if (in.read() != NodeID.CLIENT_NODE_TYPE) {
        throw new AssertionError();
      }
      this.src = new ClientID().deserializeFrom(in);
      this.tid = new TransactionID(in.readLong());
      this.oldest = new TransactionID(in.readLong());
      this.action = in.readInt();
      int length = in.readInt();
      this.payload = new byte[length];
      in.readFully(this.payload);
    } else {
      this.rid = in.readLong();
    }
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    if (getType() == RESPONSE) {
//  do nothing, just need the messageid
      out.writeLong(rid);
    } else {
      out.writeLong(rid);
      this.descriptor.serializeTo(out);
      out.writeLong(version);
      out.write(this.src.getNodeType());
      this.src.serializeTo(out);
      out.writeLong(tid.toLong());
      out.writeLong(oldest.toLong());
      out.writeInt(this.action);
      if (payload != null) {
        out.writeInt(payload.length);
        out.write(payload);
      } else {
        out.writeInt(0);
      }
    }
  }

  @Override
  public String toString() {
    return "ReplicationMessage{rid=" + rid + ", id=" + descriptor + ", src=" + src + ", tid=" + tid + ", oldest=" + oldest + ", action=" + action + '}';
  }
  
  private VoltronEntityMessage.Type decodeServerActionType(int networkType) {
    switch(networkType) {
      case NOOP:
        return null;
      case CREATE_ENTITY:
        return VoltronEntityMessage.Type.CREATE_ENTITY;
      case INVOKE_ACTION:
        return VoltronEntityMessage.Type.INVOKE_ACTION;
      case GET_ENTITY:
        return VoltronEntityMessage.Type.FETCH_ENTITY;
      case RELEASE_ENTITY:
        return VoltronEntityMessage.Type.RELEASE_ENTITY;
      case DESTROY_ENTITY:
        return VoltronEntityMessage.Type.DESTROY_ENTITY;
      case PROMOTE_ENTITY_TO_ACTIVE:
        return null;
      default:
        return null;
    }
  }
}
