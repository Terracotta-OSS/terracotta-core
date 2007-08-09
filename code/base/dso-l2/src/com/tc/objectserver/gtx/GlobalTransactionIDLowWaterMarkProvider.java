/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.gtx;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.gtx.GlobalTransactionManager;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.tx.ServerTransactionListener;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.util.State;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class GlobalTransactionIDLowWaterMarkProvider implements GlobalTransactionManager, ServerTransactionListener {

  private static final TCLogger             logger              = TCLogging
                                                                    .getLogger(GlobalTransactionIDLowWaterMarkProvider.class);
  private static final State                INITIAL             = new State("INITAL");
  private static final State                STARTED             = new State("STARTED");

  private final ServerTransactionManager    transactionManager;
  private final GlobalTransactionManager    gtxm;

  private volatile GlobalTransactionManager lwmProvider;
  private State                             state               = INITIAL;

  private final Set                         resentTxns          = new HashSet();

  private final GlobalTransactionManager    NULL_GLOBAL_TXN_MGR = new GlobalTransactionManager() {
                                                                  public GlobalTransactionID getLowGlobalTransactionIDWatermark() {
                                                                    return GlobalTransactionID.NULL_ID;
                                                                  }
                                                                };

  public GlobalTransactionIDLowWaterMarkProvider(ServerTransactionManager transactionManager,
                                                 GlobalTransactionManager gtxm) {
    this.transactionManager = transactionManager;
    this.gtxm = gtxm;
    this.lwmProvider = NULL_GLOBAL_TXN_MGR;
  }

  public void goToActiveMode() {
    transactionManager.addTransactionListener(this);
  }

  public GlobalTransactionID getLowGlobalTransactionIDWatermark() {
    return lwmProvider.getLowGlobalTransactionIDWatermark();
  }

  public synchronized void addResentServerTransactionIDs(Collection stxIDs) {
    resentTxns.addAll(stxIDs);
  }

  public synchronized void clearAllTransactionsFor(ChannelID killedClient) {
    for (Iterator i = resentTxns.iterator(); i.hasNext();) {
      ServerTransactionID sid = (ServerTransactionID) i.next();
      if (sid.getChannelID().equals(killedClient)) {
        i.remove();
      }
    }
    switchLWMProviderIfReady();
  }

  private void switchLWMProviderIfReady() {
    if (resentTxns.isEmpty() && state == STARTED) {
      logger.info("Switching GlobalTransactionID Low Water mark provider since all resent transactions are applied");
      this.lwmProvider = gtxm;
      this.transactionManager.removeTransactionListener(this);
    }
  }

  public void incomingTransactions(ChannelID cid, Set serverTxnIDs) {
    // NOP
  }

  public void transactionApplied(ServerTransactionID stxID) {
    // NOP
  }

  public synchronized void transactionCompleted(ServerTransactionID stxID) {
    resentTxns.remove(stxID);
    switchLWMProviderIfReady();
  }

  public synchronized void transactionManagerStarted(Set cids) {
    state = STARTED;
    removeAllExceptFrom(cids);
    switchLWMProviderIfReady();
  }

  private void removeAllExceptFrom(Set cids) {
    for (Iterator i = resentTxns.iterator(); i.hasNext();) {
      ServerTransactionID sid = (ServerTransactionID) i.next();
      if (!cids.contains(sid.getChannelID())) {
        i.remove();
      }
    }
  }
}
