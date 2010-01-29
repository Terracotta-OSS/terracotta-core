/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.gtx;

import com.tc.util.sequence.BatchSequenceProvider;

public interface GlobalTransactionIDSequenceProvider extends BatchSequenceProvider {

  public abstract void setNextAvailableGID(long nextGID);

  public abstract long currentGID();

}