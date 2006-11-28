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

  String getDescriptionOfLicense();

  String getBuildID();

  String getCopyright();

  L2Info[] getL2Info();

}
