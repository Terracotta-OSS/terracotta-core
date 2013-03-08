/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.io.TCByteBufferOutputStream;

import java.util.List;

interface TransactionBuffer {

  public void writeTo(TCByteBufferOutputStream dest);

  public int write(ClientTransaction txn);

  public int getTxnCount();

  public TransactionID getFoldedTransactionID();

  public void addTransactionCompleteListeners(List transactionCompleteListeners);

  public List getTransactionCompleteListeners();
}