/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.buffer;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.statistics.StatisticData;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public abstract class AbstractStatisticsBuffer implements StatisticsBuffer {
  private final static TCLogger               LOGGER                     = TCLogging.getLogger(AbstractStatisticsBuffer.class);

  private final Set<StatisticsBufferListener> listeners                  = new CopyOnWriteArraySet<StatisticsBufferListener>();

  private volatile String                     defaultAgentIp;
  private volatile String                     defaultAgentDifferentiator = null;
  
  protected AbstractStatisticsBuffer() {
    try {
      this.defaultAgentIp = InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
     throw new RuntimeException("Unexpected error while getting localhost address:", e);
    }    
  }

  public void setDefaultAgentIp(String defaultAgentIp) {
    this.defaultAgentIp = defaultAgentIp;
  }

  public void setDefaultAgentDifferentiator(String defaultAgentDifferentiator) {
    this.defaultAgentDifferentiator = defaultAgentDifferentiator;
  }

  public String getDefaultAgentIp() {
    return defaultAgentIp;
  }

  public String getDefaultAgentDifferentiator() {
    return defaultAgentDifferentiator;
  }

  public String getDefaultNodeName() {
    return defaultAgentIp + " (" + defaultAgentDifferentiator + ")";
  }

  protected void checkDefaultAgentInfo() {
    if (null == defaultAgentIp) {
      LOGGER.warn("Running without a default agent IP in the statistics buffer, this is probably due to not calling setDefaultAgentIp after creating a new buffer instance.");
    }
    if (null == defaultAgentDifferentiator) {
      LOGGER.warn("Running without a default agent differentiator in the statistics buffer, this is probably due to not calling getDefaultAgentDifferentiator after creating a new buffer instance.");
    }
  }

  public void fillInDefaultValues(final StatisticData data) {
    if (null == data.getAgentIp()) {
      data.setAgentIp(defaultAgentIp);
    }

    if (null == data.getAgentDifferentiator()) {
      data.setAgentDifferentiator(defaultAgentDifferentiator);
    }
  }

  public void addListener(final StatisticsBufferListener listener) {
    if (null == listener) { return; }

    listeners.add(listener);
  }

  public void removeListener(final StatisticsBufferListener listener) {
    if (null == listener) { return; }

    listeners.remove(listener);
  }

  protected void fireCapturingStarted(final String sessionId) {
    if (listeners.size() > 0) {
      for (StatisticsBufferListener listener : listeners) {
        listener.capturingStarted(sessionId);
      }
    }
  }

  protected void fireCapturingStopped(final String sessionId) {
    if (listeners.size() > 0) {
      for (StatisticsBufferListener listener : listeners) {
        listener.capturingStopped(sessionId);
      }
    }
  }

  protected void fireOpened() {
    if (listeners.size() > 0) {
      for (StatisticsBufferListener listener : listeners) {
        listener.opened();
      }
    }
  }

  protected void fireClosing() {
    if (listeners.size() > 0) {
      for (StatisticsBufferListener listener : listeners) {
        listener.closing();
      }
    }
  }

  protected void fireClosed() {
    if (listeners.size() > 0) {
      for (StatisticsBufferListener listener : listeners) {
        listener.closed();
      }
    }
  }
}