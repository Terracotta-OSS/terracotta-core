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

  void removeEventsForClient(ClientID clientId);

}
