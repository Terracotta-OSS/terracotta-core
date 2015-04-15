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
package com.tc.objectserver.event;

import com.google.common.collect.Multimap;
import com.tc.net.ClientID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.server.ServerEvent;

import java.util.Set;

public interface ServerEventBuffer {

  void storeEvent(GlobalTransactionID gtxId, ServerEvent serverEvent, Set<ClientID> clients);

  Multimap<ClientID, ServerEvent> getServerEventsPerClient(GlobalTransactionID gtxId);

  /**
   * Used by Passive server to clear event buffer, on basis of low watermark from clients
   */
  void clearEventBufferBelowLowWaterMark(GlobalTransactionID lowWatermark);

  /**
   * Used by Active server to remove events for a transaction, on basis of low watermark from clients
   */
  void removeEventsForTransaction(GlobalTransactionID globalTransactionID);

}
