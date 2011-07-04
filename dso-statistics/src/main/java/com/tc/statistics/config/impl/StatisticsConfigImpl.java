/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.config.impl;

import com.tc.statistics.beans.StatisticsEmitterMBean;
import com.tc.statistics.buffer.memory.MemoryStatisticsBufferImpl;
import com.tc.statistics.config.DSOStatisticsConfig;
import com.tc.statistics.retrieval.StatisticsRetriever;
import com.tc.util.Assert;
import com.tc.util.concurrent.CopyOnWriteArrayMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class StatisticsConfigImpl implements DSOStatisticsConfig {
  private final Map defaultParams;
  private final DSOStatisticsConfig parent;

  private final Map params = new CopyOnWriteArrayMap();

  public StatisticsConfigImpl() {
    // initialize default parameters
    Map defaultParamsMap = new HashMap();
    defaultParamsMap.put(KEY_RETRIEVER_SCHEDULE_INTERVAL, StatisticsRetriever.DEFAULT_GLOBAL_FREQUENCY);
    defaultParamsMap.put(KEY_EMITTER_SCHEDULE_INTERVAL, StatisticsEmitterMBean.DEFAULT_FREQUENCY);
    defaultParamsMap.put(KEY_EMITTER_BATCH_SIZE, StatisticsEmitterMBean.DEFAULT_BATCH_SIZE);
    defaultParamsMap.put(KEY_MAX_MEMORY_BUFFER_SIZE, MemoryStatisticsBufferImpl.DEFAULT_MAX_SIZE);
    defaultParamsMap.put(KEY_MEMORY_BUFFER_PURGE_PERCENTAGE, MemoryStatisticsBufferImpl.DEFAULT_PURGE_PERCENTAGE);
    defaultParams = Collections.unmodifiableMap(defaultParamsMap);

    parent = null;
  }

  private StatisticsConfigImpl(final DSOStatisticsConfig parent) {
    Assert.assertNotNull("parent", parent);
    defaultParams = Collections.EMPTY_MAP;
    this.parent = parent;
  }

  public DSOStatisticsConfig getParent() {
    return parent;
  }

  public DSOStatisticsConfig createChild() {
    return new StatisticsConfigImpl(this);
  }

  public void setParam(final String key, final Object value) {
    params.put(key, value);
  }

  public void removeParam(final String key) {
    params.remove(key);
  }

  public Object getParam(final String key) {
    Object value = params.get(key);
    if (null == value) {
      value = defaultParams.get(key);
    }
    if (null == value &&
        parent != null) {
      value = parent.getParam(key);
    }
    return value;
  }

  public long getParamLong(final String key) {
    Object value = getParam(key);
    if (null == value) {
      return 0L;
    }

    return ((Number)value).longValue();
  }

  public String getParamString(final String key) {
    Object value = getParam(key);
    if (null == value) {
      return null;
    }
    return String.valueOf(value);
  }
}
