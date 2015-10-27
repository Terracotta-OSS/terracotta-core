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

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.object.EntityID;
import java.io.IOException;

/**
 *
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
  
  public PassiveSyncMessage(EntityID id, long version, byte[] configPayload) {
    super(configPayload != null ? ENTITY_BEGIN : ENTITY_END);
    this.id = id;
    this.version = version;
    this.payload = configPayload;
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
