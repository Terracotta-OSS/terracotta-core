/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.logging.TCLogger;
import com.tc.net.NodeID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.util.ObjectIDSet;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ServerTransactionLogger implements ServerTransactionListener {

  private final TCLogger      logger;

  private final AtomicInteger outStandingTxns = new AtomicInteger(0);
  private final AtomicLong    last            = new AtomicLong(0);
  private final boolean       verboseLogging;
  private final boolean       printStatsEnabled;

  public ServerTransactionLogger(TCLogger logger, ServerTransactionManagerConfig config) {
    this.logger = logger;
    this.verboseLogging = config.isVerboseLogging();
    this.printStatsEnabled = config.isPrintStatsEnabled();
  }

  public void addResentServerTransactionIDs(Collection stxIDs) {
    logger.info("addResentTransactions: " + stxIDs);
  }

  public void clearAllTransactionsFor(NodeID deadNode) {
    logger.info("clearAllTransactionsFor: " + deadNode);
  }

  public void transactionManagerStarted(Set cids) {
    logger.info("trasactionManagerStarted: " + cids);
  }

  public void incomingTransactions(NodeID source, Set serverTxnIDs) {
    if (verboseLogging) logger.info("incomingTransactions: " + source + ", " + serverTxnIDs);
    incrementOutStandingTxns(serverTxnIDs.size());
  }

  private void incrementOutStandingTxns(int count) {
    int current = outStandingTxns.addAndGet(count);
    if (needToLogStats()) {
      logStats(current);
    }
  }

  private synchronized void decrementOutStandingTxns(int count) {
    int current = outStandingTxns.addAndGet(-count);
    if (needToLogStats()) {
      logStats(current);
    }
  }

  private boolean needToLogStats() {
    if (!printStatsEnabled) return false;
    long now = System.currentTimeMillis();
    boolean log = (now - last.get()) > 1000;
    if (log) {
      last.set(now);
    }
    return log;
  }

  private void logStats(int current) {
    logger.info("Number of pending transactions in the System : " + current);
  }

  public void transactionApplied(ServerTransactionID stxID, ObjectIDSet newObjectsCreated) {
    if (verboseLogging) logger.info("transactionApplied: " + stxID + " new Objects created : " + newObjectsCreated);
  }

  public void transactionCompleted(ServerTransactionID stxID) {
    if (verboseLogging) logger.info("transactionCompleted: " + stxID);
    decrementOutStandingTxns(1);
  }

}
