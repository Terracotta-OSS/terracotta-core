/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.config;

public interface StatisticsConfig {
  public final static String KEY_EMITTER_SCHEDULE_PERIOD = "emitter.schedule.period";
  public final static String KEY_EMITTER_BATCH_SIZE = "emitter.batch.size";
  public final static String KEY_GLOBAL_SCHEDULE_PERIOD = "global.schedule.period";

  public StatisticsConfig getParent();

  public StatisticsConfig createChild();

  public void setParam(String key, Object value);

  public Object getParam(String key);

  public long getParamLong(String key);

  public void removeParam(String key);

  public String getParamString(String key);
}
