/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.beans;

import com.tc.management.TerracottaMBean;

public interface StatisticsEmitterMBean extends TerracottaMBean {
  public final static Long DEFAULT_FREQUENCY = new Long(3000L);
  public final static Long DEFAULT_BATCH_SIZE = new Long(500L);
}