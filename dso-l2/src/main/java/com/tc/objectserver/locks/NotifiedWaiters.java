/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.locks;

import com.tc.net.NodeID;
import com.tc.object.locks.ClientServerExchangeLockContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NotifiedWaiters {

  private final Map<NodeID, Set<ClientServerExchangeLockContext>> notifiedSets = new HashMap<>();

  @Override
  public String toString() {
    synchronized (notifiedSets) {
      return "NotifiedWaiters[" + notifiedSets + "]";
    }
  }

  public boolean isEmpty() {
    return notifiedSets.isEmpty();
  }

  public void addNotification(ClientServerExchangeLockContext context) {
    synchronized (notifiedSets) {
      getOrCreateSetFor(context.getNodeID()).add(context);
    }
  }

  public Set<ClientServerExchangeLockContext> getNotifiedFor(NodeID nodeID) {
    synchronized (notifiedSets) {
      Set<ClientServerExchangeLockContext> rv = getSetFor(nodeID);
      return (rv == null) ? Collections.emptySet() : rv;
    }
  }

  private Set<ClientServerExchangeLockContext> getSetFor(NodeID nodeID) {
    return notifiedSets.get(nodeID);
  }

  private Set<ClientServerExchangeLockContext> getOrCreateSetFor(NodeID nodeID) {
    Set<ClientServerExchangeLockContext> rv = getSetFor(nodeID);
    if (rv == null) {
      rv = new HashSet<>();
      notifiedSets.put(nodeID, rv);
    }
    return rv;
  }

}
