/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.entity;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.ClientID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.tx.TransactionID;
import java.util.Set;
import com.tc.net.protocol.tcm.TCAction;


/**
 * VoltronEntityMessage is primarily used over the network but it also has server-internal "loopback" messages so this
 * interface specifically describes how the network variant would work.
 */
public interface NetworkVoltronEntityMessage extends VoltronEntityMessage, TCAction {

  public Set<VoltronEntityMessage.Acks> getRequestedAcks();
  
  public EntityID getEntityID();
  /**
   * Initializes the contents of the message.
   */
  public void setContents(ClientID clientID, TransactionID transactionID, EntityID eid, EntityDescriptor entityDescriptor, Type type, boolean requiresReplication, TCByteBuffer extendedData, TransactionID oldestTransactionPending, Set<VoltronEntityMessage.Acks> acks);

  public void setMessageCodecSupplier(MessageCodecSupplier supplier);
}
