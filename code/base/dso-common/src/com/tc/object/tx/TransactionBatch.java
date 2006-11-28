/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.bytes.TCByteBuffer;
import com.tc.lang.Recyclable;

import java.util.Collection;

public interface TransactionBatch extends Recyclable {

  public Collection getAcknowledgedTransactionIDs();
  
  public boolean isEmpty();

  public TCByteBuffer[] getData();
  
}