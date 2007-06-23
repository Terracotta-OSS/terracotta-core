/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.logging.TCLogger;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.tx.ServerTransactionID;

import java.util.Collection;
import java.util.Set;

public class ServerTransactionLogger implements ServerTransactionListener {

  private final TCLogger                       logger;
  private final ServerTransactionManagerConfig config;

  private long                                 outStandingTxns = 0;
  private long                                 last            = 0;

  public ServerTransactionLogger(TCLogger logger, ServerTransactionManagerConfig config) {
    this.logger = logger;
    this.config = config;
  }

  public void addResentServerTransactionIDs(Collection stxIDs) {
    logger.info("addResentTransactions: " + stxIDs);
  }

  public void clearAllTransactionsFor(ChannelID killedClient) {
    logger.info("clearAllTransactionsFor: " + killedClient);
  }

  public void incomingTransactions(ChannelID cid, Set serverTxnIDs) {
    if (config.isVerboseLogging()) logger.info("incomingTransactions: " + cid + ", " + serverTxnIDs);
    incrementOutStandingTxns(serverTxnIDs.size());
  }

  private void incrementOutStandingTxns(int count) {
    boolean log = needToLogStats();
    outStandingTxns += count;
    if (log) {
      logStats();
    }
  }

  private void decrementOutStandingTxns(int count) {
    outStandingTxns -= count;
    boolean log = needToLogStats();
    if (log) {
      logStats();
    }
  }

  private boolean needToLogStats() {
    if (!config.isPrintStatsEnabled()) return false;
    long now = System.currentTimeMillis();
    boolean log = (outStandingTxns == 0 || (now - last) > 1000);
    if (log) {
      last = now;
    }
    return log;
  }

  private void logStats() {
    logger.info("Number of pending transactions in the System : " + outStandingTxns);
  }

  public void transactionApplied(ServerTransactionID stxID) {
    if (config.isVerboseLogging()) logger.info("transactionApplied: " + stxID);
  }

  public void transactionCompleted(ServerTransactionID stxID) {
    if (config.isVerboseLogging()) logger.info("transactionCompleted: " + stxID);
    decrementOutStandingTxns(1);
  }
}
