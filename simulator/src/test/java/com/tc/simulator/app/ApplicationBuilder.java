/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.app;

import com.tc.simulator.listener.ListenerProvider;

public interface ApplicationBuilder {
  public Application newApplication(String applicationId, ListenerProvider listenerProvider)
      throws ApplicationInstantiationException;

  void setAppConfigAttribute(String key, String value);
}