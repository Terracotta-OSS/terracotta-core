/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.gatherer.impl;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArraySet;

import com.tc.management.JMXConnectorProxy;
import com.tc.statistics.StatisticData;
import com.tc.statistics.beans.StatisticsGatewayMBean;
import com.tc.statistics.beans.StatisticsMBeanNames;
import com.tc.statistics.gatherer.StatisticsGatherer;
import com.tc.statistics.gatherer.StatisticsGathererListener;
import com.tc.statistics.gatherer.exceptions.StatisticsGathererAlreadyConnectedException;
import com.tc.statistics.gatherer.exceptions.StatisticsGathererCloseSessionErrorException;
import com.tc.statistics.gatherer.exceptions.StatisticsGathererConnectErrorException;
import com.tc.statistics.gatherer.exceptions.StatisticsGathererConnectionRequiredException;
import com.tc.statistics.gatherer.exceptions.StatisticsGathererDisconnectErrorException;
import com.tc.statistics.gatherer.exceptions.StatisticsGathererException;
import com.tc.statistics.gatherer.exceptions.StatisticsGathererGlobalConfigGetErrorException;
import com.tc.statistics.gatherer.exceptions.StatisticsGathererGlobalConfigSetErrorException;
import com.tc.statistics.gatherer.exceptions.StatisticsGathererSessionConfigGetErrorException;
import com.tc.statistics.gatherer.exceptions.StatisticsGathererSessionConfigSetErrorException;
import com.tc.statistics.gatherer.exceptions.StatisticsGathererSessionCreationErrorException;
import com.tc.statistics.gatherer.exceptions.StatisticsGathererSessionRequiredException;
import com.tc.statistics.store.StatisticsStore;
import com.tc.statistics.store.exceptions.StatisticsStoreException;
import com.tc.util.Assert;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;

public class StatisticsGathererImpl implements StatisticsGatherer {
  private final StatisticsStore               store;
  private final Set                           listeners             = new CopyOnWriteArraySet();
  private final GathererTopologyChangeHandler topologyChangeHandler = new GathererTopologyChangeHandler();

  private volatile StatisticsGatewayMBean     statGateway           = null;
  private volatile String                     sessionId             = null;

  private JMXConnectorProxy                   proxy                 = null;
  private MBeanServerConnection               mbeanServerConnection = null;
  private StoreDataListener                   listener              = null;

  public StatisticsGathererImpl(final StatisticsStore store) {
    Assert.assertNotNull("store can't be null", store);
    this.store = store;
  }

  public void connect(final String managerHostName, final int managerPort) throws StatisticsGathererException {
    connect(null, null, managerHostName, managerPort);
  }

  public void connect(String username, String password, String managerHostName, int managerPort)
      throws StatisticsGathererException {
    synchronized (this) {
      if (statGateway != null) throw new StatisticsGathererAlreadyConnectedException();

      try {
        store.open();
      } catch (StatisticsStoreException e) {
        throw new StatisticsGathererConnectErrorException("Unexpected error while opening statistics store.", e);
      }

      final Map environment = new HashMap();
      environment.put("jmx.remote.x.server.connection.timeout", new Long(Long.MAX_VALUE));
      String[] creds = { username, password };
      environment.put("jmx.remote.credentials", creds);
      proxy = new JMXConnectorProxy(managerHostName, managerPort, environment);

      try {
        // create the server connection
        mbeanServerConnection = proxy.getMBeanServerConnection();
      } catch (Exception e) {
        throw new StatisticsGathererConnectErrorException("Unexpected error while connecting to mbean server.", e);
      }

      // setup the mbeans
      statGateway = (StatisticsGatewayMBean) MBeanServerInvocationHandler
          .newProxyInstance(mbeanServerConnection, StatisticsMBeanNames.STATISTICS_GATEWAY,
                            StatisticsGatewayMBean.class, false);

      // enable the statistics envoy
      topologyChangeHandler.setEnabled(true);
      statGateway.setTopologyChangeHandler(topologyChangeHandler);
      statGateway.enable();
    }

    fireConnected(managerHostName, managerPort);
  }

  public void disconnect() throws StatisticsGathererException {
    synchronized (this) {
      StatisticsGathererException exception = null;

      // make sure the session is closed
      try {
        closeSession();
      } catch (Exception e) {
        exception = new StatisticsGathererDisconnectErrorException(
                                                                   "Unexpected error while closing the capturing session '"
                                                                       + sessionId + "'.", e);
      }

      // disable the notification
      if (statGateway != null) {
        try {
          statGateway.disable();
        } catch (Exception e) {
          StatisticsGathererException ex = new StatisticsGathererDisconnectErrorException(
                                                                                          "Unexpected error while disabling the statistics gateway.",
                                                                                          e);
          if (exception != null) {
            exception.setNextException(ex);
          } else {
            exception = ex;
          }
        }
        statGateway.clearTopologyChangeHandler();
      }

      if (proxy != null) {
        try {
          proxy.close();
        } catch (Exception e) {
          StatisticsGathererException ex = new StatisticsGathererDisconnectErrorException(
                                                                                          "Unexpected error while closing the JMX proxy.",
                                                                                          e);
          if (exception != null) {
            exception.setNextException(ex);
          } else {
            exception = ex;
          }
        }
      }

      try {
        store.close();
      } catch (StatisticsStoreException e) {
        StatisticsGathererException ex = new StatisticsGathererDisconnectErrorException(
                                                                                        "Unexpected error while closing the statistics store.",
                                                                                        e);
        if (exception != null) {
          exception.setNextException(ex);
        } else {
          exception = ex;
        }
      }

      proxy = null;
      listener = null;
      statGateway = null;

      if (exception != null) { throw exception; }
    }

    fireDisconnected();
  }

  public void createSession(final String sessionID) throws StatisticsGathererException {
    synchronized (this) {
      if (null == statGateway) throw new StatisticsGathererConnectionRequiredException();

      closeSession();

      // create a new capturing session
      statGateway.createSession(sessionID);
      this.sessionId = sessionID;
      topologyChangeHandler.setSessionId(sessionID);
      statGateway.setTopologyChangeHandler(topologyChangeHandler);

      // register the statistics data listener
      try {
        listener = new StoreDataListener();
        mbeanServerConnection.addNotificationListener(StatisticsMBeanNames.STATISTICS_GATEWAY, listener, null, store);
      } catch (Exception e) {
        throw new StatisticsGathererSessionCreationErrorException(
                                                                  "Unexpected error while registering the notification listener for statistics emitting.",
                                                                  e);
      }
    }

    fireSessionCreated(sessionID);
  }

  public void reinitialize() throws StatisticsGathererException {
    synchronized (this) {
      if (null == statGateway) throw new StatisticsGathererConnectionRequiredException();
      closeSession();
      statGateway.reinitialize();
      sessionId = null;
    }

    fireReinitialized();
  }

  public synchronized void closeSession() throws StatisticsGathererException {
    String closed_sessionid = null;
    synchronized (this) {
      if (sessionId != null) {
        closed_sessionid = sessionId;
        stopCapturing();
        sessionId = null;
        topologyChangeHandler.setSessionId(sessionId);
        statGateway.setTopologyChangeHandler(topologyChangeHandler);

        // detach the notification listener
        try {
          mbeanServerConnection.removeNotificationListener(StatisticsMBeanNames.STATISTICS_GATEWAY, listener);
        } catch (Exception e) {
          throw new StatisticsGathererCloseSessionErrorException(
                                                                 "Unexpected error while removing the statistics gateway notification listener.",
                                                                 e);
        }
      }
    }

    if (closed_sessionid != null) {
      fireSessionClosed(closed_sessionid);
    }
  }

  public String getActiveSessionId() {
    return sessionId;
  }

  public String[] getSupportedStatistics() throws StatisticsGathererException {
    if (null == statGateway) throw new StatisticsGathererConnectionRequiredException();
    return statGateway.getSupportedStatistics();
  }

  public StatisticData[] captureStatistic(final String name) throws StatisticsGathererException {
    if (null == sessionId) throw new StatisticsGathererSessionRequiredException();
    return statGateway.captureStatistic(sessionId, name);
  }

  public StatisticData[] retrieveStatisticData(final String name) throws StatisticsGathererException {
    if (null == statGateway) throw new StatisticsGathererConnectionRequiredException();
    return statGateway.retrieveStatisticData(name);
  }

  public void enableStatistics(final String[] names) throws StatisticsGathererException {
    Assert.assertNotNull("names", names);

    if (null == sessionId) throw new StatisticsGathererSessionRequiredException();

    statGateway.disableAllStatistics(sessionId);
    for (String name : names) {
      statGateway.enableStatistic(sessionId, name);
    }

    topologyChangeHandler.setEnabledStatistics(names);
    statGateway.setTopologyChangeHandler(topologyChangeHandler);

    fireStatisticsEnabled(names);
  }

  public void startCapturing() throws StatisticsGathererException {
    if (null == sessionId) throw new StatisticsGathererSessionRequiredException();
    statGateway.startCapturing(sessionId);
    fireCapturingStarted(sessionId);

    topologyChangeHandler.setCapturingStarted(true);
    statGateway.setTopologyChangeHandler(topologyChangeHandler);
  }

  public boolean isCapturing() {
    return topologyChangeHandler.isCapturingStarted();
  }

  public void stopCapturing() throws StatisticsGathererException {
    if (null == sessionId) throw new StatisticsGathererSessionRequiredException();
    statGateway.stopCapturing(sessionId);
    fireCapturingStopped(sessionId);

    topologyChangeHandler.setCapturingStarted(false);
    statGateway.setTopologyChangeHandler(topologyChangeHandler);
  }

  public void setGlobalParam(final String key, final Object value) throws StatisticsGathererException {
    if (null == statGateway) throw new StatisticsGathererConnectionRequiredException();
    try {
      statGateway.setGlobalParam(key, value);

      topologyChangeHandler.setGlobalConfigParam(key, value);
      statGateway.setTopologyChangeHandler(topologyChangeHandler);
    } catch (Exception e) {
      throw new StatisticsGathererGlobalConfigSetErrorException(key, value, e);
    }
  }

  public Object getGlobalParam(final String key) throws StatisticsGathererException {
    if (null == statGateway) throw new StatisticsGathererConnectionRequiredException();
    try {
      return statGateway.getGlobalParam(key);
    } catch (Exception e) {
      throw new StatisticsGathererGlobalConfigGetErrorException(key, e);
    }
  }

  public void setSessionParam(final String key, final Object value) throws StatisticsGathererException {
    if (null == sessionId) throw new StatisticsGathererSessionRequiredException();
    try {
      statGateway.setSessionParam(sessionId, key, value);

      topologyChangeHandler.setSessionConfigParam(sessionId, key, value);
      statGateway.setTopologyChangeHandler(topologyChangeHandler);
    } catch (Exception e) {
      throw new StatisticsGathererSessionConfigSetErrorException(sessionId, key, value, e);
    }
  }

  public Object getSessionParam(final String key) throws StatisticsGathererException {
    if (null == sessionId) throw new StatisticsGathererSessionRequiredException();
    try {
      return statGateway.getSessionParam(sessionId, key);
    } catch (Exception e) {
      throw new StatisticsGathererSessionConfigGetErrorException(sessionId, key, e);
    }
  }

  public void addListener(final StatisticsGathererListener gathererListener) {
    if (null == gathererListener) { return; }

    listeners.add(gathererListener);
  }

  public void removeListener(final StatisticsGathererListener gathererListener) {
    if (null == gathererListener) { return; }

    listeners.remove(gathererListener);
  }

  private void fireConnected(final String managerHostName, final int managerPort) {
    if (listeners.size() > 0) {
      for (Iterator it = listeners.iterator(); it.hasNext();) {
        ((StatisticsGathererListener) it.next()).connected(managerHostName, managerPort);
      }
    }
  }

  private void fireDisconnected() {
    if (listeners.size() > 0) {
      for (Iterator it = listeners.iterator(); it.hasNext();) {
        ((StatisticsGathererListener) it.next()).disconnected();
      }
    }
  }

  private void fireReinitialized() {
    if (listeners.size() > 0) {
      for (Iterator it = listeners.iterator(); it.hasNext();) {
        ((StatisticsGathererListener) it.next()).reinitialized();
      }
    }
  }

  private void fireCapturingStarted(final String sessionID) {
    if (listeners.size() > 0) {
      for (Iterator it = listeners.iterator(); it.hasNext();) {
        ((StatisticsGathererListener) it.next()).capturingStarted(sessionID);
      }
    }
  }

  private void fireCapturingStopped(final String sessionID) {
    if (listeners.size() > 0) {
      for (Iterator it = listeners.iterator(); it.hasNext();) {
        ((StatisticsGathererListener) it.next()).capturingStopped(sessionID);
      }
    }
  }

  private void fireSessionCreated(final String sessionID) {
    if (listeners.size() > 0) {
      for (Iterator it = listeners.iterator(); it.hasNext();) {
        ((StatisticsGathererListener) it.next()).sessionCreated(sessionID);
      }
    }
  }

  private void fireSessionClosed(final String sessionID) {
    if (listeners.size() > 0) {
      for (Iterator it = listeners.iterator(); it.hasNext();) {
        ((StatisticsGathererListener) it.next()).sessionClosed(sessionID);
      }
    }
  }

  private void fireStatisticsEnabled(final String[] names) {
    if (listeners.size() > 0) {
      for (Iterator it = listeners.iterator(); it.hasNext();) {
        ((StatisticsGathererListener) it.next()).statisticsEnabled(names);
      }
    }
  }
}