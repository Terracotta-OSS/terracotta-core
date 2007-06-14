/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.l1.impl;

import com.tc.logging.TCLogger;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.objectserver.l1.api.ClientState;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.ObjectIDSet2;
import com.tc.util.State;

import java.util.Collection;
import java.util.Collections;
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

  private static final State STARTED = new State("STARTED");
  private static final State STOPPED = new State("STOPPED");

  private State              state   = STARTED;

  private final Map          clientStates;
  private final TCLogger     logger;

  // for testing
  public ClientStateManagerImpl(TCLogger logger, Map states) {
    this.logger = logger;
    this.clientStates = states;
  }

  public ClientStateManagerImpl(TCLogger logger) {
    this(logger, new HashMap());
  }

  public synchronized List createPrunedChangesAndAddObjectIDTo(Collection changes, BackReferences includeIDs,
                                                               ChannelID id, Set objectIDs) {
    assertStarted();
    ClientStateImpl clientState = getClientState(id);
    if (clientState == null) {
      logger.warn(": createPrunedChangesAndAddObjectIDTo : Client state is NULL (probably due to disconnect) : " + id);
      return Collections.EMPTY_LIST;
    }
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
    ClientStateImpl c = getClientState(id);
    if (c != null) {
      c.addReference(objectID);
    } else {
      logger.warn(": addReference : Client state is NULL (probably due to disconnect) : " + id);
    }
  }

  public synchronized void removeReferences(ChannelID id, Set removed) {
    assertStarted();
    // logger.info("Removing Reference for " + id + " to " + removed);
    ClientStateImpl c = getClientState(id);
    if (c != null) {
      c.removeReferences(removed);
    } else {
      logger.warn(": removeReferences : Client state is NULL (probably due to disconnect) : " + id);
    }
  }

  public synchronized boolean hasReference(ChannelID id, ObjectID objectID) {
    ClientStateImpl c = getClientState(id);
    if (c != null) {
      return c.containsReference(objectID);
    } else {
      logger.warn(": hasReference : Client state is NULL (probably due to disconnect) : " + id);
      return false;
    }
  }

  public synchronized void addAllReferencedIdsTo(Set ids) {
    assertStarted();
    for (Iterator i = clientStates.values().iterator(); i.hasNext();) {
      ClientStateImpl s = (ClientStateImpl) i.next();
      s.addReferencedIdsTo(ids);
    }
  }

  public synchronized void removeReferencedFrom(ChannelID id, Set oids) {
    ClientState cs = getClientState(id);
    if(cs == null) {
      logger.warn(": removeReferencedFrom : Client state is NULL (probably due to disconnect) : " + id);
      return;
    }
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
  public synchronized Set addReferences(ChannelID id, Set oids) {
    ClientState cs = getClientState(id);
    if(cs == null) {
      logger.warn(": addReferences : Client state is NULL (probably due to disconnect) : " + id);
      return Collections.EMPTY_SET;
    }
    Set refs = cs.getReferences();
    if (refs.isEmpty()) {
      refs.addAll(oids);
      return oids;
    }
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
    clientStates.remove(waitee);
  }

  public synchronized void startupClient(ChannelID clientID) {
    if (!isStarted()) { return; }
    Object old = clientStates.put(clientID, new ClientStateImpl(clientID));
    if (old != null) { throw new AssertionError("Client connected before disconnecting : old Client state = " + old); }
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

  private ClientStateImpl getClientState(ChannelID clientID) {
    return (ClientStateImpl) clientStates.get(clientID);
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
