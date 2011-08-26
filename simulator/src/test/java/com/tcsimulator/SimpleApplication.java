/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcsimulator;

import com.tc.simulator.app.Application;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

public class SimpleApplication implements Application {

  private final ApplicationConfig cfg;
  private final ListenerProvider  listenerProvider;
  private final String            appId;

  public SimpleApplication(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    this.appId = appId;
    this.cfg = cfg;
    this.listenerProvider = listenerProvider;
  }

  public String getApplicationId() {
    return this.appId;
  }

  public void run() {
    boolean result = false;

    try {
      try {
        Thread.sleep(Long.parseLong(this.cfg.getAttribute("sleepInterval")));
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      if (Boolean.getBoolean(this.cfg.getAttribute("throwException"))) { throw new RuntimeException(
                                                                                                    "I was told to do it!"); }
      result = true;
    } finally {
      listenerProvider.getResultsListener().notifyResult(Boolean.valueOf(result));
    }
  }

  public boolean interpretResult(Object o) {
    return (o instanceof Boolean) && ((Boolean) o).booleanValue();
  }

}