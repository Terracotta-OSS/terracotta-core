/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.tx;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

import com.tc.exception.TCRuntimeException;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

public final class TestTransactionBatchManager implements TransactionBatchManager {

  public final LinkedQueue defineBatchContexts = new LinkedQueue();

  public void defineBatch(ChannelID channelID, TxnBatchID batchID, int count) {
    Object[] args = new Object[] { channelID, batchID, new Integer(count) };
    try {
      defineBatchContexts.put(args);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

  public final NoExceptionLinkedQueue batchComponentCompleteCalls = new NoExceptionLinkedQueue();
  public final SynchronizedBoolean isBatchComponentComplete = new SynchronizedBoolean(false); 
  
  public boolean batchComponentComplete(ChannelID channelID, TxnBatchID batchID, TransactionID txnID) {
    batchComponentCompleteCalls.put(new Object[] {channelID, batchID, txnID });
    return isBatchComponentComplete.get();
  }

  public final NoExceptionLinkedQueue shutdownClientCalls = new NoExceptionLinkedQueue();
  public void shutdownClient(ChannelID channelID) {
    shutdownClientCalls.put(channelID);
  }

}