/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.gtx;

import com.tc.object.tx.ServerTransactionID;

public interface GlobalTransactionIDGenerator extends GlobalTransactionManager {

  public GlobalTransactionID getOrCreateGlobalTransactionID(ServerTransactionID serverTransactionID);

}
