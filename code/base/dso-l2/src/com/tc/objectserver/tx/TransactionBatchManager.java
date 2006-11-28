/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;

public interface TransactionBatchManager {

  public void defineBatch(ChannelID channelID, TxnBatchID batchID, int numTxns) throws BatchDefinedException;

  public boolean batchComponentComplete(ChannelID channelID, TxnBatchID batchID, TransactionID txnID)
      throws NoSuchBatchException;

  public void shutdownClient(ChannelID channelID);
}
