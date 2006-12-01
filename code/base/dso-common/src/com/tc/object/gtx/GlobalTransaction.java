/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.gtx;

import com.tc.util.SequenceID;

public interface GlobalTransaction {
  public GlobalTransactionID getGlobalTransactionID();
  public SequenceID getSequenceID();
}
