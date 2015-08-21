/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.beans.l1;

import com.tc.management.RuntimeStatisticConstants;
import com.tc.management.TerracottaMBean;

import java.util.Map;

import javax.management.NotificationEmitter;

public interface L1InfoMBean extends TerracottaMBean, NotificationEmitter, RuntimeStatisticConstants {
  public static final String VERBOSE_GC = "jmx.terracotta.L1.verboseGC";

  String getVersion();

  String getMavenArtifactsVersion();

  String getBuildID();

  boolean isPatched();

  String getPatchLevel();

  String getPatchVersion();

  String getPatchBuildID();

  String getClientUUID();

  String getCopyright();

  String takeThreadDump(long requestMillis);

  byte[] takeCompressedThreadDump(long requestMillis);

  String getEnvironment();

  String getConfig();

  Map<String, Object> getStatistics();

  long getUsedMemory();

  long getMaxMemory();

  boolean isVerboseGC();

  void setVerboseGC(boolean verboseGC);

  void gc();

  String getTCProperties();

  String[] getProcessArguments();
}
