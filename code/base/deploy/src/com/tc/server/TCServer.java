/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.server;

import com.tc.config.schema.L2Info;

public interface TCServer {
  void setActivationListener(TCServerActivationListener listener);

  void start() throws Exception;

  void stop();

  boolean isStarted();

  boolean isActive();

  boolean isStopped();

  long getStartTime();

  long getActivateTime();

  void shutdown();

  String getDescriptionOfCapabilities();
  
  L2Info[] infoForAllL2s();
}
