/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.ObjectRequestServerContext;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.tx.AbstractServerTransactionListener;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.util.ObjectIDSet;
import com.tc.util.State;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

public class ObjectRequestManagerRestartImpl extends AbstractServerTransactionListener implements ObjectRequestManager {

  private final static State                      INIT                 = new State("INITIAL");
  private final static State                      STARTING             = new State("STARTING");
  private final static State                      STARTED              = new State("STARTED");

  private final static TCLogger                   logger               = TCLogging
                                                                           .getLogger(ObjectRequestManagerRestartImpl.class);

  private final ObjectRequestManager              delegate;
  private final ServerTransactionManager          transactionManager;
  private final ObjectManager                     objectManager;

  private final Set                               resentTransactionIDs = Collections.synchronizedSet(new HashSet());
  private final Queue<ObjectRequestServerContext> pendingRequests      = new LinkedBlockingQueue<ObjectRequestServerContext>();
  private volatile State                          state                = INIT;

  public ObjectRequestManagerRestartImpl(ObjectManager objectMgr, ServerTransactionManager transactionManager,
                                         ObjectRequestManager delegate) {
    this.objectManager = objectMgr;
    this.delegate = delegate;
    this.transactionManager = transactionManager;
    transactionManager.addTransactionListener(this);
  }

  @Override
  public void transactionManagerStarted(Set cids) {
    this.state = STARTING;
    this.objectManager.start();
    moveToStartedIfPossible();
  }

  private void moveToStartedIfPossible() {
    if (this.state == STARTING && this.resentTransactionIDs.isEmpty()) {
      this.state = STARTED;
      this.transactionManager.removeTransactionListener(this);
      processPending();
    }
  }

  @Override
  public void addResentServerTransactionIDs(Collection sTxIDs) {
    if (this.state != INIT) { throw new AssertionError("Cant add Resent transactions after start up ! " + sTxIDs.size()
                                                       + "Txns : " + this.state); }
    this.resentTransactionIDs.addAll(sTxIDs);
    logger.info("resentTransactions = " + this.resentTransactionIDs.size());
  }

  @Override
  public void transactionApplied(ServerTransactionID stxID, ObjectIDSet newObjectsCreated) {
    this.resentTransactionIDs.remove(stxID);
    moveToStartedIfPossible();
  }

  @Override
  public void clearAllTransactionsFor(NodeID client) {
    if (this.state == STARTED) { return; }
    synchronized (this.resentTransactionIDs) {
      for (Iterator iter = this.resentTransactionIDs.iterator(); iter.hasNext();) {
        ServerTransactionID stxID = (ServerTransactionID) iter.next();
        if (stxID.getSourceID().equals(client)) {
          iter.remove();
        }
      }
    }
    moveToStartedIfPossible();
  }

  private void processPending() {
    logger.info("Processing Pending Lookups = " + this.pendingRequests.size());
    ObjectRequestServerContext lookupContext;
    while ((lookupContext = this.pendingRequests.poll()) != null) {
      logger.info("Processing pending Looking up : " + toString(lookupContext));
      this.delegate.requestObjects(lookupContext);
    }
  }

  public String toString(ObjectRequestServerContext c) {
    return c.getClass().getName() + " [ " + c.getClientID() + " :  " + c.getRequestedObjectIDs() + " : "
           + c.getRequestID() + " : " + c.getRequestDepth() + " : " + c.getRequestingThreadName() + "] ";
  }

  public void requestObjects(ObjectRequestServerContext requestContext) {
    if (this.state != STARTED) {
      this.pendingRequests.add(requestContext);
      if (logger.isDebugEnabled()) {
        logger.debug("RequestObjectManager is not started, lookup has been added to pending request: "
                     + toString(requestContext));
      }
      return;
    }
    this.delegate.requestObjects(requestContext);
  }

  public void sendObjects(ClientID requestedNodeID, Collection objs, ObjectIDSet requestedObjectIDs,
                          ObjectIDSet missingObjectIDs, boolean isServerInitiated, int maxRequestDepth) {
    this.delegate.sendObjects(requestedNodeID, objs, requestedObjectIDs, missingObjectIDs, isServerInitiated,
                              maxRequestDepth);
  }

}
