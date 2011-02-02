/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.handler.LockInfoDumpHandler;
import com.tc.management.beans.TCDumper;
import com.tc.net.core.ClusterTopologyChangedListener;

public interface TCClient extends TCDumper, LockInfoDumpHandler {

  public void startBeanShell(int port);

  public void reloadConfiguration() throws ConfigurationSetupException;

  public void addServerConfigurationChangedListeners(ClusterTopologyChangedListener listener);

  public String[] processArguments();
}
