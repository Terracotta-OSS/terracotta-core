/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.inmemory;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.objectserver.persistence.api.ClientStatePersistor;
import com.tc.util.sequence.MutableSequence;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InMemoryClientStatePersistor implements ClientStatePersistor {

  private final Map             clients              = new HashMap();
  private final MutableSequence connectionIDSequence = new InMemorySequenceProvider();

  public MutableSequence getConnectionIDSequence() {
    return this.connectionIDSequence;
  }

  public Set loadClientIDs() {
    synchronized (this.clients) {
      return new HashSet(this.clients.keySet());
    }
  }

  public boolean containsClient(final ChannelID id) {
    synchronized (this.clients) {
      return this.clients.containsKey(id);
    }
  }

  public void saveClientState(final ChannelID cid) {
    synchronized (this.clients) {
      if (!containsClient(cid)) {
        this.clients.put(cid, new Object());
      }
    }
  }

  public void deleteClientState(final ChannelID id) throws ClientNotFoundException {
    Object removed = null;
    synchronized (this.clients) {
      removed = this.clients.remove(id);
    }
    if (removed == null) { throw new ClientNotFoundException("Client not found: " + id); }
  }

}
