/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.object.EntityID;
import java.io.IOException;

/**
 *
 * @author mscott
 */
public class PassiveSyncMessage extends AbstractGroupMessage {
//  message types  
  public static final int BEGIN               = 0; // Sent to replicate a request on the passive
  public static final int END                = 1; // response that the replicated action completed

  public static final int ENTITY_BEGIN       = 2;
  public static final int ENTITY_END         = 3;
  
  public static final int ENTITY_CONCURRENCY_BEGIN = 4;
  public static final int ENTITY_CONCURRENCY_END = 5;
  
  public static final int ENTITY_CONCURRENCY_PAYLOAD = 6;
  
  EntityID id;
  long version;
  int concurrency;
  byte[] payload;

  public PassiveSyncMessage() {
    super(0);
    id = new EntityID("", "");
  }
  
  public PassiveSyncMessage(boolean start) {
    super(start ? BEGIN : END);
    id = new EntityID("", "");
  }
  
  public PassiveSyncMessage(EntityID id, long version, boolean start) {
    super(start ? ENTITY_BEGIN : ENTITY_END);
    this.id = id;
    this.version = version;
  }
  
  public PassiveSyncMessage(EntityID id, int concurrency, boolean start) {
    super(start ? ENTITY_CONCURRENCY_BEGIN : ENTITY_CONCURRENCY_END);
    this.id = id;
    this.concurrency = concurrency;
  }

  public PassiveSyncMessage(EntityID id, int concurrency, byte[] payload) {
    super(ENTITY_CONCURRENCY_PAYLOAD);
    this.id = id;
    this.concurrency = concurrency;
    this.payload = payload;
  }  

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    this.id = new EntityID(in.readUTF(), in.readUTF());
    this.version = in.readLong();
    this.concurrency = in.readInt();
    this.payload = new byte[in.readInt()];
    in.readFully(this.payload);
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    try {
      out.writeUTF(id.getClassName());
      out.writeUTF(id.getEntityName());
      out.writeLong(version);
      out.writeInt(concurrency);
      out.writeInt(payload == null ? 0 : payload.length);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }
  
  public EntityID getEntityID() {
    return id;
  }
  
  public long getVersion() {
    return version;
  }
  
  public byte[] getPayload() {
    return payload;
  }
  
}
