/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

  @Override
  public boolean isEmpty() {
    throw new ImplementMe();
  }

  @Override
  public TCByteBuffer[] getData() {
    return batchData;
  }

  @Override
  public void recycle() {
    return;
  }

}
