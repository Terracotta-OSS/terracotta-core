/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.dna.api.LogicalChangeID;

import java.util.List;
import java.util.Map;

public interface TransactionBuffer {

  public void writeTo(TCByteBufferOutputStream dest);

  public int write(ClientTransaction txn);

  public int getTxnCount();

  public TransactionID getFoldedTransactionID();

  public void addTransactionCompleteListeners(List transactionCompleteListeners);

  public List getTransactionCompleteListeners();

  public LogicalChangeListener getLogicalChangeListenerFor(LogicalChangeID id);

  public void addLogicalChangeListeners(Map<LogicalChangeID, LogicalChangeListener> logicalChangeListeners);
}