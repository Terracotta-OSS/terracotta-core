/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.gtx;

import com.tc.object.tx.ServerTransactionID;

public interface GlobalTransactionIDGenerator extends GlobalTransactionManager {

  public GlobalTransactionID getOrCreateGlobalTransactionID(ServerTransactionID serverTransactionID);

}
