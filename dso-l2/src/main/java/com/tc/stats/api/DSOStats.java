/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.stats.api;

public interface DSOStats {

  Number[] getStatistics(String[] names);

  long getTransactionRate();

  long getReadOperationRate();

  long getEvictionRate();

  long getExpirationRate();

  long getGlobalLockRecallRate();

  long getTransactionSizeRate();

  long getBroadcastRate();

  long getGlobalServerMapGetSizeRequestsCount();

  long getGlobalServerMapGetValueRequestsCount();

  long getGlobalServerMapGetSizeRequestsRate();

  long getGlobalServerMapGetValueRequestsRate();

  long getWriteOperationRate();

}
