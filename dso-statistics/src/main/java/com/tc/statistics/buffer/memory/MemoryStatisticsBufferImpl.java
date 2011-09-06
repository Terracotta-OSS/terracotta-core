/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.buffer.memory;

import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticsSystemType;
import com.tc.statistics.buffer.AbstractStatisticsBuffer;
import com.tc.statistics.buffer.StatisticsConsumer;
import com.tc.statistics.buffer.exceptions.StatisticsBufferException;
import com.tc.statistics.buffer.exceptions.StatisticsBufferStartCapturingSessionNotFoundException;
import com.tc.statistics.buffer.exceptions.StatisticsBufferStopCapturingSessionNotFoundException;
import com.tc.statistics.buffer.exceptions.StatisticsBufferUnknownCaptureSessionException;
import com.tc.statistics.config.DSOStatisticsConfig;
import com.tc.statistics.retrieval.StatisticsRetriever;
import com.tc.statistics.retrieval.impl.StatisticsRetrieverImpl;
import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MemoryStatisticsBufferImpl extends AbstractStatisticsBuffer {
  public final static Long                                 DEFAULT_MAX_SIZE         = Long.valueOf(20000L);
  public final static Long                                 DEFAULT_PURGE_PERCENTAGE = Long.valueOf(10);

  private final StatisticsSystemType                       type;
  private final DSOStatisticsConfig                        config;

  private final ConcurrentMap<String, List<StatisticData>> buffer                   = new ConcurrentHashMap<String, List<StatisticData>>();
  private final ConcurrentMap<String, Date>                sessions                 = new ConcurrentHashMap<String, Date>();

  public MemoryStatisticsBufferImpl(final StatisticsSystemType type, final DSOStatisticsConfig config) {
    super();

    Assert.assertNotNull("type", type);
    Assert.assertNotNull("config", config);

    this.type = type;
    this.config = config;
  }

  public void open() throws StatisticsBufferException {
    if (StatisticsSystemType.CLIENT == type
        && TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.CVT_CLIENT_FAIL_BUFFER_OPEN, false)) { throw new StatisticsBufferException(
                                                                                                                                                     "Forcibly failing opening the statistics buffer through the "
                                                                                                                                                         + TCPropertiesConsts.CVT_CLIENT_FAIL_BUFFER_OPEN
                                                                                                                                                         + " property",
                                                                                                                                                     null); }

    fireOpened();
  }

  public void close() {
    fireClosing();
    fireClosed();
  }

  public void reinitialize() {
    synchronized (this) {
      buffer.clear();
    }
  }

  public StatisticsRetriever createCaptureSession(final String sessionId) {
    checkDefaultAgentInfo();
    Assert.assertNotNull("sessionId", sessionId);

    final List<StatisticData> newSessionDataList = new ArrayList<StatisticData>();
    buffer.putIfAbsent(sessionId, newSessionDataList);

    return new StatisticsRetrieverImpl(config.createChild(), this, sessionId);
  }

  public void startCapturing(final String sessionId) throws StatisticsBufferException {
    if (!buffer.containsKey(sessionId)) { throw new StatisticsBufferStartCapturingSessionNotFoundException(sessionId); }

    sessions.putIfAbsent(sessionId, new Date());

    fireCapturingStarted(sessionId);
  }

  public void stopCapturing(final String sessionId) throws StatisticsBufferException {
    if (!buffer.containsKey(sessionId)) { throw new StatisticsBufferStopCapturingSessionNotFoundException(sessionId); }

    if (sessions.remove(sessionId) != null) {
      fireCapturingStopped(sessionId);
    }
  }

  public void storeStatistic(final StatisticData data) throws StatisticsBufferException {
    Assert.assertNotNull("data", data);
    Assert.assertNotNull("sessionId property of data", data.getSessionId());
    Assert.assertNotNull("moment property of data", data.getMoment());
    Assert.assertNotNull("name property of data", data.getName());

    fillInDefaultValues(data);

    Assert.assertNotNull("agentIp property of data", data.getAgentIp());
    Assert.assertNotNull("agentDifferentiator property of data", data.getAgentDifferentiator());

    final List<StatisticData> sessionDataList = buffer.get(data.getSessionId());
    if (null == sessionDataList) { throw new StatisticsBufferUnknownCaptureSessionException(data.getSessionId(), null); }

    synchronized (sessionDataList) {
      // using >= instead of == explicitly for defensive coding, you never know if a bug makes the size grow larger that
      // the max allowed value
      if (sessionDataList.size() >= config.getParamLong(DSOStatisticsConfig.KEY_MAX_MEMORY_BUFFER_SIZE)) {
        final int amountToRemove = (int) ((sessionDataList.size() / 100) * config
            .getParamLong(DSOStatisticsConfig.KEY_MEMORY_BUFFER_PURGE_PERCENTAGE));
        final ListIterator<StatisticData> it = sessionDataList.listIterator(0);
        for (int i = 0; i < amountToRemove; i++) {
          it.next();
          it.remove();
        }
      }

      sessionDataList.add(data);
    }
  }

  public void consumeStatistics(final String sessionId, final StatisticsConsumer consumer)
      throws StatisticsBufferException {
    Assert.assertNotNull("sessionId", sessionId);
    Assert.assertNotNull("consumer", consumer);

    final List<StatisticData> newSessionDataList = new ArrayList<StatisticData>();
    final List<StatisticData> oldSessionDataList = buffer.replace(sessionId, newSessionDataList);
    if (null == oldSessionDataList) { throw new StatisticsBufferUnknownCaptureSessionException(sessionId, null); }

    final boolean limit_consumption = consumer.getMaximumConsumedDataCount() > 0;

    try {
      synchronized (oldSessionDataList) {
        long consumedCount = 0;
        final Iterator<StatisticData> it = oldSessionDataList.iterator();
        while (it.hasNext() && (!limit_consumption || consumedCount < consumer.getMaximumConsumedDataCount())) {
          if (!consumer.consumeStatisticData(it.next())) { return; }
          it.remove();
          consumedCount++;
        }
      }
    } finally {
      // make the statistic data that wasn't consumed during this consumption phase
      // available again so that it can be picked up by another consumption operation
      synchronized (newSessionDataList) {
        newSessionDataList.addAll(0, oldSessionDataList);
      }
    }

  }
}