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
import com.tc.util.Assert;

import java.io.IOException;


public class PassiveSyncMessage extends AbstractGroupMessage {
  public static PassiveSyncMessage createStartSyncMessage() {
    EntityID fakeID = new EntityID("", "");
    long fakeVersion = 1;
    return new PassiveSyncMessage(BEGIN, fakeID, fakeVersion, 0, null);
  }
  public static PassiveSyncMessage createEndSyncMessage() {
    EntityID fakeID = new EntityID("", "");
    long fakeVersion = 1;
    return new PassiveSyncMessage(END, fakeID, fakeVersion, 0, null);
  }
  public static PassiveSyncMessage createStartEntityMessage(EntityID id, long version, byte[] configPayload) {
    return new PassiveSyncMessage(ENTITY_BEGIN, id, version, 0, configPayload);
  }
  public static PassiveSyncMessage createEndEntityMessage(EntityID id, long version) {
    return new PassiveSyncMessage(ENTITY_END, id, version, 0, null);
  }
  public static PassiveSyncMessage createStartEntityKeyMessage(EntityID id, long version, int concurrency) {
    // TEMP
    Assert.assertTrue(1 == concurrency);
    return new PassiveSyncMessage(ENTITY_CONCURRENCY_BEGIN, id, version, concurrency, null);
  }
  public static PassiveSyncMessage createEndEntityKeyMessage(EntityID id, long version, int concurrency) {
    // TEMP
    Assert.assertTrue(1 == concurrency);
    return new PassiveSyncMessage(ENTITY_CONCURRENCY_END, id, version, concurrency, null);
  }
  public static PassiveSyncMessage createPayloadMessage(EntityID id, long version, int concurrency, byte[] payload) {
    // TEMP
    Assert.assertTrue(1 == concurrency);
    return new PassiveSyncMessage(ENTITY_CONCURRENCY_PAYLOAD, id, version, concurrency, payload);
  }


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
    super(-1);
    // Serialization support.
  }

  private PassiveSyncMessage(int messageType, EntityID id, long version, int concurrencyKey, byte[] payload) {
    super(messageType);
    Assert.assertNotNull(id);
    this.id = id;
    // We always need a valid version and those start at 1.
    Assert.assertTrue(version > 0);
    this.version = version;
    this.concurrency = concurrencyKey;
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
      if (null != payload) {
        out.write(payload);
      }
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
