/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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

  @Override
  public void addResentServerTransactionIDs(Collection stxIDs) {
    logger.info("addResentTransactions: " + stxIDs);
  }

  @Override
  public void clearAllTransactionsFor(NodeID deadNode) {
    logger.info("clearAllTransactionsFor: " + deadNode);
  }

  @Override
  public void transactionManagerStarted(Set cids) {
    logger.info("trasactionManagerStarted: " + cids);
  }

  @Override
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

  @Override
  public void transactionApplied(ServerTransactionID stxID, ObjectIDSet newObjectsCreated) {
    if (verboseLogging) logger.info("transactionApplied: " + stxID + " new Objects created : " + newObjectsCreated);
  }

  @Override
  public void transactionCompleted(ServerTransactionID stxID) {
    if (verboseLogging) logger.info("transactionCompleted: " + stxID);
    decrementOutStandingTxns(1);
  }

}
