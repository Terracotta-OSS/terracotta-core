/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
