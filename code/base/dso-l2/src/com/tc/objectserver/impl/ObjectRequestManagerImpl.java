/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.context.ManagedObjectRequestContext;
import com.tc.objectserver.tx.ServerTransactionListener;
import com.tc.objectserver.tx.ServerTransactionManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ObjectRequestManagerImpl implements ObjectRequestManager, ServerTransactionListener {

  private final static TCLogger logger               = TCLogging.getLogger(ObjectRequestManagerImpl.class);

  private final Set             resentTransactionIDs = new HashSet();
  private final ObjectManager   objectManager;
  private final List            pendingRequests      = Collections.synchronizedList(new LinkedList());

  public ObjectRequestManagerImpl(ObjectManager objectManager, ServerTransactionManager transactionManager) {
    this.objectManager = objectManager;
    transactionManager.addTransactionListener(this);

  }

  public void requestObjects(Collection requestedIDs, ManagedObjectRequestContext responseContext,
                             int maxReachableObjects) {
    synchronized (this) {
      if (!resentTransactionIDs.isEmpty()) {
        pendingRequests.add(new PendingRequest(requestedIDs, responseContext, maxReachableObjects));
        return;
      }
    }
    objectManager.lookupObjectsAndSubObjectsFor(responseContext.getChannelID(), requestedIDs, responseContext,
                                                maxReachableObjects);
  }

  public synchronized void addResentServerTransactionIDs(Collection sTxIDs) {
    resentTransactionIDs.addAll(sTxIDs);
    logger.info("resentTransactions = " + resentTransactionIDs);
  }

  public void transactionCompleted(ServerTransactionID stxID) {
    return;
  }

  public void clearAllTransactionsFor(ChannelID client) {
    synchronized (this) {
      if (resentTransactionIDs.isEmpty()) return;
      for (Iterator iter = resentTransactionIDs.iterator(); iter.hasNext();) {
        ServerTransactionID stxID = (ServerTransactionID) iter.next();
        if (stxID.getChannelID().equals(client)) {
          iter.remove();
        }
      }
      if (!resentTransactionIDs.isEmpty()) return;
    }
    processPending();
  }

  private void processPending() {
    List copy;
    synchronized (pendingRequests) {
      copy = new ArrayList(pendingRequests);
      pendingRequests.clear();
    }
    logger.info("Processing Pending Lookups = " + copy.size());
    for (Iterator iter = copy.iterator(); iter.hasNext();) {
      PendingRequest request = (PendingRequest) iter.next();
      objectManager.lookupObjectsAndSubObjectsFor(request.getResponseContext().getChannelID(), request
          .getRequestedIDs(), request.getResponseContext(), request.getMaxReachableObjects());
    }
  }

  public void transactionApplied(ServerTransactionID stxID) {
    synchronized (this) {
      if (resentTransactionIDs.isEmpty()) return;
      if (resentTransactionIDs.remove(stxID) && !resentTransactionIDs.isEmpty()) { return; }
    }
    processPending();
  }

  public static class PendingRequest {

    private final Collection                  requestedIDs;
    private final ManagedObjectRequestContext responseContext;
    private final int                         maxReachableObjects;

    public PendingRequest(Collection requestedIDs, ManagedObjectRequestContext responseContext, int maxReachableObjects) {
      this.requestedIDs = requestedIDs;
      this.responseContext = responseContext;
      this.maxReachableObjects = maxReachableObjects;
    }

    public int getMaxReachableObjects() {
      return maxReachableObjects;
    }

    public Collection getRequestedIDs() {
      return requestedIDs;
    }

    public ManagedObjectRequestContext getResponseContext() {
      return responseContext;
    }
  }
}
