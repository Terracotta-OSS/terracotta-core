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

package com.tc.entity;

import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;
import java.util.Set;


/**
 * VoltronEntityMessage is primarily used over the network but it also has server-internal "loopback" messages so this
 * interface specifically describes how the network variant would work.
 */
public interface NetworkVoltronEntityMessage extends VoltronEntityMessage, TCMessage {

  public Set<VoltronEntityMessage.Acks> getRequestedAcks();
  /**
   * Initializes the contents of the message.
   */
  public void setContents(ClientID clientID, TransactionID transactionID, EntityDescriptor entityDescriptor, Type type, boolean requiresReplication, byte[] extendedData, TransactionID oldestTransactionPending, Set<VoltronEntityMessage.Acks> acks);

  public void setMessageCodecSupplier(MessageCodecSupplier supplier);
}
