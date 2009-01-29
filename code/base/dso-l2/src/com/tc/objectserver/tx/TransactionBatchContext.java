/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.NodeID;
import com.tc.object.msg.CommitTransactionMessage;

import java.util.LinkedHashMap;

public interface TransactionBatchContext {

  public abstract CommitTransactionMessage getCommitTransactionMessage();

  public abstract NodeID getSourceNodeID();

  public abstract long[] getHighWatermark();

  public abstract int getNumTxns();

  public abstract LinkedHashMap getTransactions();

}