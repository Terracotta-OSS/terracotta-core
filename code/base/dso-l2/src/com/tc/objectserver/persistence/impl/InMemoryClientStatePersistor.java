/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.ConnectionIdFactory;
import com.tc.net.protocol.transport.DefaultConnectionIdFactory;
import com.tc.objectserver.l1.api.ClientState;
import com.tc.objectserver.persistence.api.ClientStatePersistor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class InMemoryClientStatePersistor implements ClientStatePersistor {

  private final DefaultConnectionIdFactory connectionIDFactory = new DefaultConnectionIdFactory();
  private final Map                        clients             = new HashMap();

  public ConnectionIdFactory getConnectionIDFactory() {
    return this.connectionIDFactory;
  }

  public long nextChangeIDFor(ChannelID id) throws ClientNotFoundException {
    SynchronizedLong txIDs = null;
    synchronized (clients) {
      txIDs = (SynchronizedLong) clients.get(id);
    }
    if (txIDs == null) throw new ClientNotFoundException("Client not found: " + id);
    return txIDs.increment();
  }

  public Iterator loadClientIDs() {
    synchronized (clients) {
      return new HashSet(clients.keySet()).iterator();
    }
  }

  public boolean containsClient(ChannelID id) {
    synchronized (clients) {
      return clients.containsKey(id);
    }
  }

  public void saveClientState(ClientState clientState) {
    synchronized (clients) {
      if (!containsClient(clientState.getClientID())) clients.put(clientState.getClientID(), new SynchronizedLong(0));
    }
  }

  public void deleteClientState(ChannelID id) throws ClientNotFoundException {
    Object removed = null;
    synchronized (clients) {
      removed = clients.remove(id);
    }
    if (removed == null) throw new ClientNotFoundException("Client not found: " + id);
  }

  public Set loadConnectionIDs() {
    Set connectionIDs = new HashSet();
    for (Iterator i = loadClientIDs(); i.hasNext();) {
      connectionIDs.add(new ConnectionID(((ChannelID) i.next()).toLong(), connectionIDFactory.getServerID()));
    }
    return connectionIDs;
  }

}
