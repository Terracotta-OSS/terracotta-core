/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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