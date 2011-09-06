/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.config;

public interface DSOStatisticsConfig {
  public final static String KEY_EMITTER_SCHEDULE_INTERVAL = "emitter.schedule.interval";
  public final static String KEY_EMITTER_BATCH_SIZE = "emitter.batch.size";
  public final static String KEY_RETRIEVER_SCHEDULE_INTERVAL = "retriever.schedule.interval";
  public final static String KEY_MAX_MEMORY_BUFFER_SIZE = "max.memory.buffer.size";
  public final static String KEY_MEMORY_BUFFER_PURGE_PERCENTAGE = "memory.buffer.purge.percentage";

  public DSOStatisticsConfig getParent();

  public DSOStatisticsConfig createChild();

  public void setParam(String key, Object value);

  public Object getParam(String key);

  public long getParamLong(String key);

  public void removeParam(String key);

  public String getParamString(String key);
}
