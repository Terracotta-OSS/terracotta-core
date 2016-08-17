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

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.tx.TransactionID;
import java.util.Map;


/**
 * The generic super-interface for the entity response types since SEDA requires that each thread only process one type of message.
 * This means that the caller needs to down-cast to the specific sub-type, cased on getAckType.
 * In the future, it would be ideal to remove this in favor of a different SEDA implementation.
 */
public interface VoltronEntityMultiResponse extends TCMessage {
  TransactionID[] getReceivedTransactions();
  TransactionID[] getRetiredTransactions();
  Map<TransactionID, byte[]> getResults();
  boolean addReceived(TransactionID tid);
  boolean addRetired(TransactionID tid);
  boolean addResult(TransactionID tid, byte[] result);
}
