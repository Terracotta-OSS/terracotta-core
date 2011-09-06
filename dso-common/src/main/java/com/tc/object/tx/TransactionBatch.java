/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx;

import com.tc.bytes.TCByteBuffer;
import com.tc.lang.Recyclable;

public interface TransactionBatch extends Recyclable {

  public boolean isEmpty();

  public TCByteBuffer[] getData();
  
}