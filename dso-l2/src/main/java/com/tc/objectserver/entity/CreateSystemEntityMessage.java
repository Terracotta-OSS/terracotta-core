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
package com.tc.objectserver.entity;

import com.tc.bytes.TCByteBuffer;
import com.tc.entity.VoltronEntityMessage;
import com.tc.net.ClientID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;
import org.terracotta.entity.EntityMessage;

/**
 *
 */
public class CreateSystemEntityMessage implements VoltronEntityMessage {
  
  private final EntityID eid;
  private final long version;
  private final TCByteBuffer data;
  
  public CreateSystemEntityMessage(EntityID eid, int version, TCByteBuffer extended) {
    Assert.assertNotNull(extended);
    this.eid = eid;
    this.version = version;
    this.data = extended == null || extended.isReadOnly() ? extended : extended.asReadOnlyBuffer();
  }
  
  @Override
  public ClientID getSource() {
    return ClientID.NULL_ID;
  }

  @Override
  public TransactionID getTransactionID() {
    return TransactionID.NULL_ID;
  }

  @Override
  public EntityDescriptor getEntityDescriptor() {
    return EntityDescriptor.createDescriptorForLifecycle(eid, version);
  }

  @Override
  public boolean doesRequireReplication() {
    return true;
  }

  @Override
  public boolean doesRequestReceived() {
    return false;
  }

  @Override
  public boolean doesRequestRetired() {
    return false;
  }
  
  @Override
  public Type getVoltronType() {
    return VoltronEntityMessage.Type.CREATE_ENTITY;
  }

  @Override
  public TCByteBuffer getExtendedData() {
    return data == null ? data : data.duplicate();
  }

  @Override
  public TransactionID getOldestTransactionOnClient() {
    return TransactionID.NULL_ID;
  }

  @Override
  public EntityMessage getEntityMessage() {
    return null;
  }
}
