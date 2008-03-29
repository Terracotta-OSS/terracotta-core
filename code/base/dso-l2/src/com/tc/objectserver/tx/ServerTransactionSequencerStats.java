/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.tx;

public interface ServerTransactionSequencerStats {
  
  public String dumpPendingTxns();
  
  public String dumpTxnQ();
  
  public String dumpBlockedQ();
  
  public String dumpLocks();
  
  public String dumpObjects();
  
  public String reconcileStatus();


}
