/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.beans.impl;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.AbstractTerracottaMBean;
import com.tc.statistics.AgentStatisticsManager;
import com.tc.statistics.DynamicSRA;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.beans.StatisticsManagerMBean;
import com.tc.statistics.beans.exceptions.UnknownStatisticsSessionIdException;
import com.tc.statistics.buffer.StatisticsBuffer;
import com.tc.statistics.buffer.exceptions.StatisticsBufferException;
import com.tc.statistics.buffer.exceptions.StatisticsBufferStartCapturingSessionNotFoundException;
import com.tc.statistics.buffer.exceptions.StatisticsBufferStopCapturingSessionNotFoundException;
import com.tc.statistics.config.DSOStatisticsConfig;
import com.tc.statistics.exceptions.AgentStatisticsManagerException;
import com.tc.statistics.exceptions.StatisticDataInjectionErrorException;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;
import com.tc.statistics.retrieval.StatisticsRetriever;
import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.NotCompliantMBeanException;

public class StatisticsManagerMBeanImpl extends AbstractTerracottaMBean implements StatisticsManagerMBean, AgentStatisticsManager {
  private final static TCLogger LOGGER = TCLogging.getLogger(StatisticsManagerMBeanImpl.class);

  private final DSOStatisticsConfig config;
  private final StatisticsRetrievalRegistry registry;
  private final StatisticsBuffer buffer;
  private final Map retrieverMap = new ConcurrentHashMap();

  public StatisticsManagerMBeanImpl(final DSOStatisticsConfig config, final StatisticsRetrievalRegistry registry, final StatisticsBuffer buffer) throws NotCompliantMBeanException {
    super(StatisticsManagerMBean.class, false);

    Assert.assertNotNull("config", config);
    Assert.assertNotNull("registry", registry);
    Assert.assertNotNull("buffer", buffer);

    this.config = config;
    this.registry = registry;
    this.buffer = buffer;
  }

  public void reset() {
    //
  }

  public synchronized void reinitialize() {
    boolean was_enabled = isEnabled();

    disable();

    Set session_ids = retrieverMap.keySet();
    for (Iterator it = session_ids.iterator(); it.hasNext();) {
      stopCapturing((String)it.next());
    }

    try {
      buffer.reinitialize();
    } catch (StatisticsBufferException e) {
      throw new RuntimeException("Unexpected error while reinitializing the buffer.");
    }

    if (was_enabled) {
      enable();
    }
  }

  public String[] getSupportedStatistics() {
    Collection stats = registry.getSupportedStatistics();
    return (String[])stats.toArray(new String[stats.size()]);
  }

  public synchronized void createSession(final String sessionId) {
    try {
      if (retrieverMap.containsKey(sessionId)) {
        LOGGER.warn("The capture session with ID '" + sessionId + "' already exists, not creating it again.");
      }

      StatisticsRetriever retriever = buffer.createCaptureSession(sessionId);
      retrieverMap.put(sessionId, retriever);
    } catch (StatisticsBufferException e) {
      throw new RuntimeException("Unexpected error while creating a new capture session.", e);
    }
  }

  public synchronized void disableAllStatistics(final String sessionId) {
    StatisticsRetriever retriever = obtainRetriever(sessionId);
    retriever.removeAllActions();
    cleanUpStatisticsCollection();
  }

  public synchronized boolean enableStatistic(final String sessionId, final String name) {
    StatisticsRetriever retriever = obtainRetriever(sessionId);
    StatisticRetrievalAction action = registry.getActionInstance(name);
    if (null == action) {
      return false;
    }
    retriever.registerAction(action);
    enableStatisticsCollection(action);
    return true;
  }

  public synchronized String getStatisticType(String name) {
    StatisticRetrievalAction action = registry.getActionInstance(name);
    if (null == action) {
      return null;
    }

    return action.getType().toString();
  }

  public StatisticData[] captureStatistic(final String sessionId, final String name) {
    // obtain the retriever to make sure that the provided session ID is active
    obtainRetriever(sessionId);

    StatisticRetrievalAction action = registry.getActionInstance(name);
    if (null == action) {
      return null;
    }

    final Date moment = new Date();
    StatisticData[] data = action.retrieveStatisticData();
    if (data != null) {
      for (int i = 0; i < data.length; i++) {
        data[i].setSessionId(sessionId);
        data[i].setMoment(moment);
        try {
          buffer.storeStatistic(data[i]);
        } catch (StatisticsBufferException e) {
          throw new RuntimeException("Error while storing the statistic data '" + name + "' for cluster-wide ID '" + sessionId + "'.", e);
        }
      }
    } else {
      data = StatisticData.EMPTY_ARRAY;
    }
    return data;
  }

  public StatisticData[] retrieveStatisticData(final String name) {
    StatisticRetrievalAction action = registry.getActionInstance(name);
    if (null == action) {
      return null;
    }

    final Date moment = new Date();
    StatisticData[] data = action.retrieveStatisticData();
    if (data != null) {
      for (int i = 0; i < data.length; i++) {
        buffer.fillInDefaultValues(data[i]);
        data[i].setMoment(moment);
      }
    } else {
      data = StatisticData.EMPTY_ARRAY;
    }
    return data;
  }

  public synchronized void startCapturing(final String sessionId) {
    try {
      buffer.startCapturing(sessionId);
    } catch (StatisticsBufferStartCapturingSessionNotFoundException e) {
      throw new UnknownStatisticsSessionIdException(buffer.getDefaultNodeName(), e.getSessionId(), e);
    } catch (StatisticsBufferException e) {
      throw new RuntimeException("Error while starting the capture session with cluster-wide ID '" + sessionId + "'.", e);
    }
  }

  public synchronized void stopCapturing(final String sessionId) {
    try {
      retrieverMap.remove(sessionId);
      buffer.stopCapturing(sessionId);
      cleanUpStatisticsCollection();
    } catch (StatisticsBufferStopCapturingSessionNotFoundException e) {
      throw new UnknownStatisticsSessionIdException(buffer.getDefaultNodeName(), e.getSessionId(), e);
    } catch (StatisticsBufferException e) {
      throw new RuntimeException("Error while stopping the capture session with cluster-wide ID '" + sessionId + "'.", e);
    }
  }

  public void setGlobalParam(final String key, final Object value) {
    config.setParam(key, value);
  }

  public Object getGlobalParam(final String key) {
    return config.getParam(key);
  }

  public synchronized void setSessionParam(final String sessionId, final String key, final Object value) {
    StatisticsRetriever retriever = obtainRetriever(sessionId);
    retriever.getConfig().setParam(key, value);
  }

  public synchronized Object getSessionParam(final String sessionId, final String key) {
    StatisticsRetriever retriever = obtainRetriever(sessionId);
    return retriever.getConfig().getParam(key);
  }

  public Collection getActiveSessionIDsForAction(final String actionName) {
    return getActiveSessionsForAction(registry.getActionInstance(actionName));
  }

  private Collection getActiveSessionsForAction(final StatisticRetrievalAction action) {
    if (null == action) return Collections.EMPTY_LIST;
    final List sessions = new ArrayList();
    synchronized (this) {
      for (Iterator it = retrieverMap.keySet().iterator(); it.hasNext();) {
        String sessionId = (String)it.next();
        StatisticsRetriever retriever = (StatisticsRetriever)retrieverMap.get(sessionId);
        if (retriever.containsAction(action)) {
          sessions.add(sessionId);
        }
      }
    }
    return Collections.unmodifiableList(sessions);
  }

  public void injectStatisticData(final String sessionId, final StatisticData data) throws AgentStatisticsManagerException {
    if (!retrieverMap.containsKey(sessionId)) {
      throw new StatisticDataInjectionErrorException(sessionId, data, new UnknownStatisticsSessionIdException(buffer.getDefaultNodeName(), sessionId, null));
    }

    try {
      data.setSessionId(sessionId);
      buffer.storeStatistic(data);
    } catch (StatisticsBufferException e) {
      throw new StatisticDataInjectionErrorException(sessionId, data, e);
    }
  }

  StatisticsRetriever obtainRetriever(final String sessionId) {
    StatisticsRetriever retriever = (StatisticsRetriever)retrieverMap.get(sessionId);
    if (null == retriever) {
      throw new UnknownStatisticsSessionIdException(buffer.getDefaultNodeName(), sessionId, null);
    }
    return retriever;
  }

  private synchronized void enableStatisticsCollection(final StatisticRetrievalAction action) {
    if (null == action) {
      return;
    }
    if (action instanceof DynamicSRA) {
      ((DynamicSRA)action).enableStatisticCollection();
    }
  }

  private synchronized void cleanUpStatisticsCollection() {
    // iterate through all actions
    for (Iterator it_sra = registry.getRegisteredActionInstances().iterator(); it_sra.hasNext();) {
      StatisticRetrievalAction action = (StatisticRetrievalAction)it_sra.next();

      if (action instanceof DynamicSRA) {
        boolean disableCollection = true;

        // iterate through active sessions and if no session is using the action, disable the collection
        for (Iterator it_retrievers = retrieverMap.values().iterator(); it_retrievers.hasNext();) {
          StatisticsRetriever retriever = (StatisticsRetriever)it_retrievers.next();
          if (retriever.containsAction(action)) {
            disableCollection = false;
            break;
          }
        }

        if (disableCollection) {
          ((DynamicSRA)action).disableStatisticCollection();
        }
      }
    }
  }
}