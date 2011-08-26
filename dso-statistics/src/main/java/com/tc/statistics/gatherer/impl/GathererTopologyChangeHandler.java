/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.gatherer.impl;

import com.tc.statistics.StatisticsManager;
import com.tc.statistics.beans.TopologyChangeHandler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class GathererTopologyChangeHandler implements TopologyChangeHandler {
  private static final long serialVersionUID = 3359131234179021690L;

  private volatile boolean enabled = false;
  private volatile String sessionId = null;
  private volatile String[] enabledStatistics = null;
  private volatile boolean capturingStarted = false;

  private final Map globalConfigParams = new HashMap();
  private final Map sessionConfigParams = new HashMap();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(final String sessionId) {
    if (this.sessionId != null &&
        !this.sessionId.equals(sessionId)) {
      synchronized (sessionConfigParams) {
        sessionConfigParams.clear();
      }
    }
    this.sessionId = sessionId;
  }

  public String[] getEnabledStatistics() {
    return enabledStatistics;
  }

  public void setEnabledStatistics(final String[] enabledStatistics) {
    this.enabledStatistics = enabledStatistics;
  }

  public boolean isCapturingStarted() {
    return capturingStarted;
  }

  public void setCapturingStarted(boolean capturingStarted) {
    this.capturingStarted = capturingStarted;
  }

  public void setGlobalConfigParam(final String key, final Object value) {
    synchronized (globalConfigParams) {
      globalConfigParams.put(key, value);
    }
  }

  public void setSessionConfigParam(final String sessionId, final String key, final Object value) {
    if (null == this.sessionId ||
        !this.sessionId.equals(sessionId)) {
      return;
    }
    synchronized (sessionConfigParams) {
      sessionConfigParams.put(key, value);
    }
  }

  public void agentAdded(final StatisticsManager agent) {
    if (enabled) {
      agent.enable();
    } else {
      agent.disable();
    }

    synchronized (globalConfigParams) {
      if (globalConfigParams.size() > 0) {
        Map.Entry entry;
        for (Iterator it = globalConfigParams.entrySet().iterator(); it.hasNext(); ) {
          entry = (Map.Entry)it.next();
          agent.setGlobalParam((String)entry.getKey(), entry.getValue());
        }
      }
    }

    if (sessionId != null) {
      agent.createSession(sessionId);

      synchronized (sessionConfigParams) {
        if (sessionConfigParams.size() > 0) {
          Map.Entry entry;
          for (Iterator it = sessionConfigParams.entrySet().iterator(); it.hasNext(); ) {
            entry = (Map.Entry)it.next();
            agent.setSessionParam(sessionId, (String)entry.getKey(), entry.getValue());
          }
        }
      }

      if (enabledStatistics != null) {
        agent.disableAllStatistics(sessionId);
        for (int i = 0; i < enabledStatistics.length; i++) {
          agent.enableStatistic(sessionId, enabledStatistics[i]);
        }
      }
      if (capturingStarted) {
        agent.startCapturing(sessionId);
      }
    }
  }
}