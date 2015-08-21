/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.gtx;

import com.tc.util.SequenceID;

public interface GlobalTransaction {

  public void setGlobalTransactionID(GlobalTransactionID gid) throws GlobalTransactionIDAlreadySetException;

  public GlobalTransactionID getGlobalTransactionID();

  public SequenceID getClientSequenceID();
}
