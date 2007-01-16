/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management.beans;

import com.tc.config.schema.L2Info;
import com.tc.management.TerracottaMBean;

public interface TCServerInfoMBean extends TerracottaMBean {

  boolean isStarted();

  boolean isActive();

  long getStartTime();

  long getActivateTime();

  void stop();

  void shutdown();

  String getVersion();

  String getBuildID();

  String getCopyright();

  L2Info[] getL2Info();

}
