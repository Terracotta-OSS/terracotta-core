/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.objectserver.persistence.api.ClientStatePersistor;
import com.tc.util.sequence.MutableSequence;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InMemoryClientStatePersistor implements ClientStatePersistor {

  private final Map                        clients             = new HashMap();
  private final MutableSequence         connectionIDSequence = new InMemorySequenceProvider();

  public MutableSequence getConnectionIDSequence() {
    return this.connectionIDSequence;
  }

  public Set loadClientIDs() {
    synchronized (clients) {
      return new HashSet(clients.keySet());
    }
  }

  public boolean containsClient(ChannelID id) {
    synchronized (clients) {
      return clients.containsKey(id);
    }
  }

  public void saveClientState(ChannelID cid) {
    synchronized (clients) {
      if (!containsClient(cid)) clients.put(cid, new Object());
    }
  }

  public void deleteClientState(ChannelID id) throws ClientNotFoundException {
    Object removed = null;
    synchronized (clients) {
      removed = clients.remove(id);
    }
    if (removed == null) throw new ClientNotFoundException("Client not found: " + id);
  }

}
