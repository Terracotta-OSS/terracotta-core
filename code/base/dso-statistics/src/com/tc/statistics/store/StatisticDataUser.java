/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.store;

import com.tc.statistics.StatisticData;

public interface StatisticDataUser {
  public boolean useStatisticData(StatisticData data);
}
