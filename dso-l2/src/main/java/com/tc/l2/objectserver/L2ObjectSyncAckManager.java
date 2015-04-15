/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.l2.objectserver;

import com.tc.net.groups.MessageID;
import com.tc.object.tx.ServerTransactionID;

public interface L2ObjectSyncAckManager {

  /**
   * Startup the object sync with sync messages coming from a particular node.
   */
  public void reset();

  /**
   * Add an object sync message to be ACKed upon completion.
   */
  public void addObjectSyncMessageToAck(final ServerTransactionID stxnID, final MessageID requestID);

  /**
   * Complete the object sync
   */
  public void objectSyncComplete();

  /**
   * ACK the object sync txn inline for use when the object sync transaction is ignored (when the L2 is already
   * PASSIVE-STANDBY).
   */
  public void ackObjectSyncTxn(final ServerTransactionID stxnID);
}
