/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.l1.impl;

import com.tc.logging.TCLogger;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.objectserver.l1.api.ClientState;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.objectserver.persistence.api.ClientStatePersistor;
import com.tc.objectserver.persistence.impl.ClientNotFoundException;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.ObjectIDSet2;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author steve
 */
public class ClientStateManagerImpl implements ClientStateManager {

  private static final State         STARTED = new State("STARTED");
  private static final State         STOPPED = new State("STOPPED");

  private State                      state   = STARTED;

  private final Map                  clientStates;
  private final ClientStatePersistor store;
  private final TCLogger             logger;

  // for testing
  public ClientStateManagerImpl(TCLogger logger, Map states, ClientStatePersistor store) {
    this.logger = logger;
    this.clientStates = states;
    this.store = store;
    for (Iterator i = store.loadClientIDs(); i.hasNext();) {
      getOrCreateClientState((ChannelID) i.next());
    }
  }

  public ClientStateManagerImpl(TCLogger logger, ClientStatePersistor store) {
    this(logger, new HashMap(), store);
  }

  public synchronized List createPrunedChangesAndAddObjectIDTo(Collection changes, BackReferences includeIDs,
                                                               ChannelID id, Set objectIDs) {
    assertStarted();
    ClientStateImpl clientState = getOrCreateClientState(id);
    LinkedList prunedChanges = new LinkedList();

    for (Iterator i = changes.iterator(); i.hasNext();) {
      DNA dna = (DNA) i.next();
      if (clientState.containsReference(dna.getObjectID())) {
        if (dna.isDelta()) {
          prunedChanges.add(dna);
        } else {
          // This new Object must have already been sent as a part of a different lookup. So ignoring this change.
        }
        // } else if (clientState.containsParent(dna.getObjectID(), includeIDs)) {
        // these objects needs to be looked up from the client during apply
        // objectIDs.add(dna.getObjectID());
      }
    }
    clientState.addReferencedChildrenTo(objectIDs, includeIDs);

    return prunedChanges;
  }

  public synchronized void addReference(ChannelID id, ObjectID objectID) {
    assertStarted();
    // logger.info("Adding Reference for " + id + " to " + objectID);
    ClientStateImpl c = getOrCreateClientState(id);
    c.addReference(objectID);
  }

  public synchronized void removeReferences(ChannelID id, Set removed) {
    assertStarted();
    // logger.info("Removing Reference for " + id + " to " + removed);
    ClientStateImpl c = getOrCreateClientState(id);
    c.removeReferences(removed);
  }

  public synchronized void addAllReferencedIdsTo(Set ids) {
    assertStarted();
    for (Iterator i = clientStates.values().iterator(); i.hasNext();) {
      ClientStateImpl s = (ClientStateImpl) i.next();
      s.addReferencedIdsTo(ids);
    }
  }

  public synchronized void removeReferencedFrom(ChannelID channelID, Set oids) {
    ClientState cs = getOrCreateClientState(channelID);
    Set refs = cs.getReferences();
    // XXX:: This is a work around for THashSet's poor implementation of removeAll
    if (oids.size() >= refs.size()) {
      oids.removeAll(refs);
    } else {
      for (Iterator i = oids.iterator(); i.hasNext();) {
        if (refs.contains(i.next())) {
          i.remove();
        }
      }
    }

  }

  /*
   * returns newly added references
   */
  public synchronized Set addReferences(ChannelID channelID, Set oids) {
    ClientState cs = getOrCreateClientState(channelID);
    Set refs = cs.getReferences();
    Set newReferences = new HashSet();
    for (Iterator i = oids.iterator(); i.hasNext();) {
      Object oid = i.next();
      if (refs.add(oid)) {
        newReferences.add(oid);
      }
    }
    return newReferences;
  }

  public synchronized void shutdownClient(ChannelID waitee) {
    if (!isStarted()) {
      // it's too late to remove the client from the database. On startup, this guy will fail to reconnect
      // within the timeout period and be slain.
      return;
    }

    Object removed = clientStates.remove(waitee);
    try {
      if (removed != null) store.deleteClientState(waitee);
    } catch (ClientNotFoundException e) {
      throw new AssertionError(e);
    }
  }

  public synchronized void stop() {
    assertStarted();
    state = STOPPED;
    logger.info("ClientStateManager stopped.");
  }

  private boolean isStarted() {
    return state == STARTED;
  }

  private void assertStarted() {
    if (state != STARTED) throw new AssertionError("Not started.");
  }

  public synchronized Collection getAllClientIDs() {
    Collection rv = new HashSet();
    for (Iterator i = clientStates.values().iterator(); i.hasNext();) {
      rv.add(((ClientState) i.next()).getClientID());
    }
    return rv;
  }

  private ClientStateImpl getOrCreateClientState(ChannelID clientID) {
    ClientStateImpl clientState;
    if ((clientState = (ClientStateImpl) clientStates.get(clientID)) == null) {
      clientState = new ClientStateImpl(clientID);
      clientStates.put(clientID, clientState);
      store.saveClientState(clientState);
    }
    return clientState;
  }

  public synchronized PrettyPrinter prettyPrint(PrettyPrinter out) {
    PrettyPrinter rv = out;
    out.println(getClass().getName());
    out = out.duplicateAndIndent();
    out.indent().println("client states: ");
    out = out.duplicateAndIndent();
    for (Iterator i = clientStates.keySet().iterator(); i.hasNext();) {
      Object key = i.next();
      ClientStateImpl st = (ClientStateImpl) clientStates.get(key);
      out.indent().print(key + "=").visit(st).println();
    }
    return rv;
  }

  private static class State {
    private final String name;

    private State(String name) {
      this.name = name;
    }

    public String toString() {
      return getClass().getName() + "[" + this.name + "]";
    }
  }

  private static class ClientStateImpl implements PrettyPrintable, ClientState {
    private final ChannelID clientID;
    private final Set       managed = new ObjectIDSet2();

    public ClientStateImpl(ChannelID clientID) {
      this.clientID = clientID;
    }

    public void addReferencedChildrenTo(Set objectIDs, BackReferences includeIDs) {
      Set parents = includeIDs.getAllParents();
      parents.retainAll(managed);
      includeIDs.addReferencedChildrenTo(objectIDs, parents);
    }

    public String toString() {
      return "ClientStateImpl[" + clientID + ", " + managed + "]";
    }

    public Set getReferences() {
      return managed;
    }

    public PrettyPrinter prettyPrint(PrettyPrinter out) {
      out.println(getClass().getName());
      out.duplicateAndIndent().indent().print("managed: ").visit(managed);
      return out;
    }

    public void addReference(ObjectID id) {
      managed.add(id);
    }

    public boolean containsReference(ObjectID id) {
      return managed.contains(id);
    }

    public void removeReferences(Set references) {
      managed.removeAll(references);
    }

    public void addReferencedIdsTo(Set ids) {
      ids.addAll(managed);
    }

    public ChannelID getClientID() {
      return clientID;
    }
  }

}
