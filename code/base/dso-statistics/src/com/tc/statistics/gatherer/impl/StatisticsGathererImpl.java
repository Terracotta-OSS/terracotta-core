/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.gatherer.impl;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArraySet;

import com.tc.management.JMXConnectorProxy;
import com.tc.statistics.beans.StatisticsGatewayMBean;
import com.tc.statistics.beans.StatisticsMBeanNames;
import com.tc.statistics.gatherer.StatisticsGatherer;
import com.tc.statistics.gatherer.StatisticsGathererListener;
import com.tc.statistics.gatherer.exceptions.TCStatisticsGathererAlreadyConnectedException;
import com.tc.statistics.gatherer.exceptions.TCStatisticsGathererCloseSessionErrorException;
import com.tc.statistics.gatherer.exceptions.TCStatisticsGathererConnectionRequiredException;
import com.tc.statistics.gatherer.exceptions.TCStatisticsGathererException;
import com.tc.statistics.gatherer.exceptions.TCStatisticsGathererGlobalConfigGetErrorException;
import com.tc.statistics.gatherer.exceptions.TCStatisticsGathererGlobalConfigSetErrorException;
import com.tc.statistics.gatherer.exceptions.TCStatisticsGathererSessionConfigGetErrorException;
import com.tc.statistics.gatherer.exceptions.TCStatisticsGathererSessionConfigSetErrorException;
import com.tc.statistics.gatherer.exceptions.TCStatisticsGathererSessionCreationErrorException;
import com.tc.statistics.gatherer.exceptions.TCStatisticsGathererSessionRequiredException;
import com.tc.statistics.store.StatisticsStore;
import com.tc.statistics.store.exceptions.TCStatisticsStoreException;
import com.tc.statistics.StatisticData;
import com.tc.util.Assert;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;

public class StatisticsGathererImpl implements StatisticsGatherer {
  private final StatisticsStore store;
  private final Set listeners = new CopyOnWriteArraySet();
  private final GathererTopologyChangeHandler topologyChangeHandler = new GathererTopologyChangeHandler();

  private volatile StatisticsGatewayMBean statGateway = null;
  private volatile String sessionId = null;

  private JMXConnectorProxy proxy = null;
  private MBeanServerConnection mbeanServerConnection = null;
  private StoreDataListener listener = null;

  public StatisticsGathererImpl(final StatisticsStore store) {
    Assert.assertNotNull("store can't be null", store);
    this.store = store;
  }

  public void connect(final String managerHostName, final int managerPort) throws TCStatisticsGathererException {
    synchronized (this) {
      if (statGateway != null) throw new TCStatisticsGathererAlreadyConnectedException();

      try {
        store.open();
      } catch (TCStatisticsStoreException e) {
        throw new TCStatisticsGathererSessionCreationErrorException("Unexpected error while opening statistics store.", e);
      }

      final Map environment = new HashMap();
      environment.put("jmx.remote.x.server.connection.timeout", new Long(Long.MAX_VALUE));
      proxy = new JMXConnectorProxy(managerHostName, managerPort, environment);

      try {
        // create the server connection
        mbeanServerConnection = proxy.getMBeanServerConnection();
      } catch (Exception e) {
        throw new TCStatisticsGathererSessionCreationErrorException("Unexpected error while connecting to mbean server.", e);
      }

      // setup the mbeans
      statGateway = (StatisticsGatewayMBean)MBeanServerInvocationHandler
          .newProxyInstance(mbeanServerConnection, StatisticsMBeanNames.STATISTICS_GATEWAY, StatisticsGatewayMBean.class, false);

      // enable the statistics envoy
      statGateway.enable();
      topologyChangeHandler.setEnabled(true);
      statGateway.setTopologyChangeHandler(topologyChangeHandler);
    }

    fireConnected(managerHostName, managerPort);
  }

  public void disconnect() throws TCStatisticsGathererException {
    synchronized (this) {
      TCStatisticsGathererException exception = null;

      // make sure the session is closed
      try {
        closeSession();
      } catch (Exception e) {
        exception = new TCStatisticsGathererCloseSessionErrorException("Unexpected error while closing the capturing session '"+sessionId+"'.", e);
      }

      // disable the notification
      if (statGateway != null) {
        try {
          statGateway.disable();
        } catch (Exception e) {
          TCStatisticsGathererException ex = new TCStatisticsGathererCloseSessionErrorException("Unexpected error while disabling the statistics gateway.", e);
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
          TCStatisticsGathererException ex = new TCStatisticsGathererCloseSessionErrorException("Unexpected error while closing the JMX proxy.", e);
          if (exception != null) {
            exception.setNextException(ex);
          } else {
            exception = ex;
          }
        }
      }

      try {
        store.close();
      } catch (TCStatisticsStoreException e) {
        TCStatisticsGathererException ex = new TCStatisticsGathererCloseSessionErrorException("Unexpected error while closing the statistics store.", e);
        if (exception != null) {
          exception.setNextException(ex);
        } else {
          exception = ex;
        }
      }

      proxy = null;
      listener = null;
      statGateway = null;

      if (exception != null) {
        throw exception;
      }
    }

    fireDisconnected();
  }

  public void createSession(final String sessionId) throws TCStatisticsGathererException {
    synchronized (this) {
      if (null == statGateway) throw new TCStatisticsGathererConnectionRequiredException();

      closeSession();

      // create a new capturing session
      statGateway.createSession(sessionId);
      this.sessionId = sessionId;
      topologyChangeHandler.setSessionId(sessionId);
      statGateway.setTopologyChangeHandler(topologyChangeHandler);

      // register the statistics data listener
      try {
        listener = new StoreDataListener();
        mbeanServerConnection.addNotificationListener(StatisticsMBeanNames.STATISTICS_GATEWAY, listener, null, store);
      } catch (Exception e) {
        throw new TCStatisticsGathererSessionCreationErrorException("Unexpected error while registering the notification listener for statistics emitting.", e);
      }
    }

    fireSessionCreated(sessionId);
  }

  public void reinitialize() throws TCStatisticsGathererException {
    synchronized (this) {
      closeSession();
      statGateway.reinitialize();
      sessionId = null;
    }
    
    fireReinitialized();
  }

  public synchronized void closeSession() throws TCStatisticsGathererException {
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
          throw new TCStatisticsGathererCloseSessionErrorException("Unexpected error while removing the statistics gateway notification listener.", e);
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

  public String[] getSupportedStatistics() throws TCStatisticsGathererException {
    if (null == statGateway) throw new TCStatisticsGathererConnectionRequiredException();
    return statGateway.getSupportedStatistics();
  }

  public StatisticData[] captureStatistic(String name) throws TCStatisticsGathererException {
    if (null == sessionId) throw new TCStatisticsGathererSessionRequiredException();
    return statGateway.captureStatistic(sessionId, name);
  }

  public void enableStatistics(final String[] names) throws TCStatisticsGathererException {
    Assert.assertNotNull("names", names);

    if (null == sessionId) throw new TCStatisticsGathererSessionRequiredException();

    statGateway.disableAllStatistics(sessionId);
    for (int i = 0; i < names.length; i++) {
      statGateway.enableStatistic(sessionId, names[i]);
    }

    topologyChangeHandler.setEnabledStatistics(names);
    statGateway.setTopologyChangeHandler(topologyChangeHandler);
  }

  public void startCapturing() throws TCStatisticsGathererException {
    if (null == sessionId) throw new TCStatisticsGathererSessionRequiredException();
    statGateway.startCapturing(sessionId);
    fireCapturingStarted(sessionId);

    topologyChangeHandler.setCapturingStarted(true);
    statGateway.setTopologyChangeHandler(topologyChangeHandler);
  }

  public void stopCapturing() throws TCStatisticsGathererException {
    if (null == sessionId) throw new TCStatisticsGathererSessionRequiredException();
    statGateway.stopCapturing(sessionId);
    fireCapturingStopped(sessionId);

    topologyChangeHandler.setCapturingStarted(false);
    statGateway.setTopologyChangeHandler(topologyChangeHandler);
  }

  public void setGlobalParam(final String key, final Object value) throws TCStatisticsGathererException {
    if (null == statGateway) throw new TCStatisticsGathererConnectionRequiredException();
    try {
      statGateway.setGlobalParam(key, value);

      topologyChangeHandler.setGlobalConfigParam(key, value);
      statGateway.setTopologyChangeHandler(topologyChangeHandler);
    } catch (Exception e) {
      throw new TCStatisticsGathererGlobalConfigSetErrorException(key, value, e);
    }
  }

  public Object getGlobalParam(final String key) throws TCStatisticsGathererException {
    if (null == statGateway) throw new TCStatisticsGathererConnectionRequiredException();
    try {
      return statGateway.getGlobalParam(key);
    } catch (Exception e) {
      throw new TCStatisticsGathererGlobalConfigGetErrorException(key, e);
    }
  }

  public void setSessionParam(final String key, final Object value) throws TCStatisticsGathererException {
    if (null == sessionId) throw new TCStatisticsGathererSessionRequiredException();
    try {
      statGateway.setSessionParam(sessionId, key, value);

      topologyChangeHandler.setSessionConfigParam(sessionId, key, value);
      statGateway.setTopologyChangeHandler(topologyChangeHandler);
    } catch (Exception e) {
      throw new TCStatisticsGathererSessionConfigSetErrorException(sessionId, key, value, e);
    }
  }

  public Object getSessionParam(final String key) throws TCStatisticsGathererException {
    if (null == sessionId) throw new TCStatisticsGathererSessionRequiredException();
    try {
      return statGateway.getSessionParam(sessionId, key);
    } catch (Exception e) {
      throw new TCStatisticsGathererSessionConfigGetErrorException(sessionId, key, e);
    }
  }

  public void addListener(final StatisticsGathererListener listener) {
    if (null == listener) {
      return;
    }

    listeners.add(listener);
  }

  public void removeListener(final StatisticsGathererListener listener) {
    if (null == listener) {
      return;
    }

    listeners.remove(listener);
  }

  private void fireConnected(final String managerHostName, final int managerPort) {
    if (listeners.size() > 0) {
      for (Iterator it = listeners.iterator(); it.hasNext(); ) {
        ((StatisticsGathererListener)it.next()).connected(managerHostName, managerPort);
      }
    }
  }

  private void fireDisconnected() {
    if (listeners.size() > 0) {
      for (Iterator it = listeners.iterator(); it.hasNext(); ) {
        ((StatisticsGathererListener)it.next()).disconnected();
      }
    }
  }

  private void fireReinitialized() {
    if (listeners.size() > 0) {
      for (Iterator it = listeners.iterator(); it.hasNext(); ) {
        ((StatisticsGathererListener)it.next()).reinitialized();
      }
    }
  }

  private void fireCapturingStarted(final String sessionId) {
    if (listeners.size() > 0) {
      for (Iterator it = listeners.iterator(); it.hasNext(); ) {
        ((StatisticsGathererListener)it.next()).capturingStarted(sessionId);
      }
    }
  }

  private void fireCapturingStopped(final String sessionId) {
    if (listeners.size() > 0) {
      for (Iterator it = listeners.iterator(); it.hasNext(); ) {
        ((StatisticsGathererListener)it.next()).capturingStopped(sessionId);
      }
    }
  }

  private void fireSessionCreated(final String sessionId) {
    if (listeners.size() > 0) {
      for (Iterator it = listeners.iterator(); it.hasNext(); ) {
        ((StatisticsGathererListener)it.next()).sessionCreated(sessionId);
      }
    }
  }

  private void fireSessionClosed(final String sessionId) {
    if (listeners.size() > 0) {
      for (Iterator it = listeners.iterator(); it.hasNext(); ) {
        ((StatisticsGathererListener)it.next()).sessionClosed(sessionId);
      }
    }
  }
}