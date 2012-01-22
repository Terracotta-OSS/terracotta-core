/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.bytes.TCByteBuffer;
import com.tc.lang.Recyclable;

public interface TransactionBatch extends Recyclable {

  public boolean isEmpty();

  public TCByteBuffer[] getData();
  
}
