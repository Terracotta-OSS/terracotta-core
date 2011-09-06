/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.exception.ImplementMe;
import com.tc.object.tx.TransactionBatch;

public class TestTransactionBatch implements TransactionBatch {

  private final TCByteBuffer[] batchData;

  public TestTransactionBatch(TCByteBuffer[] batchData) {
    this.batchData = batchData;
  }

  public boolean isEmpty() {
    throw new ImplementMe();
  }

  public TCByteBuffer[] getData() {
    return batchData;
  }

  public void recycle() {
    return;
  }

}
