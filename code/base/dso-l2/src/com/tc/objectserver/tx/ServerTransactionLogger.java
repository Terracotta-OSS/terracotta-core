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

  private final TCLogger logger;

  public ServerTransactionLogger(TCLogger logger) {
    this.logger = logger;
  }

  public void addResentServerTransactionIDs(Collection stxIDs) {
    logger.info("addResentTransactions: " + stxIDs);
  }

  public void clearAllTransactionsFor(ChannelID killedClient) {
    logger.info("clearAllTransactionsFor: " + killedClient);
  }

  public void incomingTransactions(ChannelID cid, Set serverTxnIDs) {
    logger.info("incomingTransactions: " + cid + ", " + serverTxnIDs);
  }

  public void transactionApplied(ServerTransactionID stxID) {
    logger.info("transactionApplied: " + stxID);
  }

  public void transactionCompleted(ServerTransactionID stxID) {
    logger.info("transactionCompleted: " + stxID);
  }

}
