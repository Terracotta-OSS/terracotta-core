/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
import com.tc.util.State;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ObjectRequestManagerImpl implements ObjectRequestManager, ServerTransactionListener {

  private final static TCLogger          logger               = TCLogging.getLogger(ObjectRequestManagerImpl.class);

  private final static State             INIT                 = new State("INITIAL");
  private final static State             STARTING             = new State("STARTING");
  private final static State             STARTED              = new State("STARTED");

  private final ObjectManager            objectManager;
  private final ServerTransactionManager transactionManager;

  private final List                     pendingRequests      = new LinkedList();
  private final Set                      resentTransactionIDs = new HashSet();
  private volatile State                 state                = INIT;

  public ObjectRequestManagerImpl(ObjectManager objectManager, ServerTransactionManager transactionManager) {
    this.objectManager = objectManager;
    this.transactionManager = transactionManager;
    transactionManager.addTransactionListener(this);

  }

  public synchronized void transactionManagerStarted(Set cids) {
    state = STARTING;
    objectManager.start();
    moveToStartedIfPossible();
  }

  private void moveToStartedIfPossible() {
    if (state == STARTING && resentTransactionIDs.isEmpty()) {
      state = STARTED;
      transactionManager.removeTransactionListener(this);
      processPending();
    }
  }

  public void requestObjects(ManagedObjectRequestContext responseContext, int maxReachableObjects) {
    synchronized (this) {
      if (state != STARTED) {
        pendingRequests.add(new PendingRequest(responseContext, maxReachableObjects));
        return;
      }
    }
    objectManager.lookupObjectsAndSubObjectsFor(responseContext.getChannelID(), responseContext, maxReachableObjects);
  }

  public synchronized void addResentServerTransactionIDs(Collection sTxIDs) {
    if (state != INIT) { throw new AssertionError("Cant add Resent transactions after start up ! " + sTxIDs.size()
                                                  + "Txns : " + state); }
    resentTransactionIDs.addAll(sTxIDs);
    logger.info("resentTransactions = " + resentTransactionIDs.size());
  }

  public void transactionCompleted(ServerTransactionID stxID) {
    return;
  }

  public void incomingTransactions(ChannelID cid, Set serverTxnIDs) {
    return;
  }

  public synchronized void clearAllTransactionsFor(ChannelID client) {
    if (state == STARTED) return;
    for (Iterator iter = resentTransactionIDs.iterator(); iter.hasNext();) {
      ServerTransactionID stxID = (ServerTransactionID) iter.next();
      if (stxID.getChannelID().equals(client)) {
        iter.remove();
      }
    }
    moveToStartedIfPossible();
  }

  private void processPending() {
    logger.info("Processing Pending Lookups = " + pendingRequests.size());
    for (Iterator iter = pendingRequests.iterator(); iter.hasNext();) {
      PendingRequest request = (PendingRequest) iter.next();
      logger.info("Processing pending Looking up : " + request.getResponseContext());
      objectManager.lookupObjectsAndSubObjectsFor(request.getResponseContext().getChannelID(), request
          .getResponseContext(), request.getMaxReachableObjects());
    }
  }

  public synchronized void transactionApplied(ServerTransactionID stxID) {
    resentTransactionIDs.remove(stxID);
    moveToStartedIfPossible();
  }

  private final static class PendingRequest {

    private final ManagedObjectRequestContext responseContext;
    private final int                         maxReachableObjects;

    public PendingRequest(ManagedObjectRequestContext responseContext, int maxReachableObjects) {
      this.responseContext = responseContext;
      this.maxReachableObjects = maxReachableObjects;
    }

    public int getMaxReachableObjects() {
      return maxReachableObjects;
    }

    public ManagedObjectRequestContext getResponseContext() {
      return responseContext;
    }
  }


}
