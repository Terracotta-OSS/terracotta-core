/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.stats;

public interface DSOStats {

  Number[] getStatistics(String[] names);

  double getCacheHitRatio();

  long getCacheMissRate();

  long getTransactionRate();

  long getObjectFaultRate();

  long getObjectFlushRate();

  long getGlobalLockRecallRate();
  
  long getTransactionSizeRate();
  
  long getBroadcastRate();

}
