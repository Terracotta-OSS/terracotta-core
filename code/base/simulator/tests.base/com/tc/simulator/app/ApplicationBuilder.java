/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.simulator.app;

import com.tc.simulator.listener.ListenerProvider;

public interface ApplicationBuilder {
  public Application newApplication(String applicationId, ListenerProvider listenerProvider)
      throws ApplicationInstantiationException;

  public ClassLoader getContextClassLoader();
}