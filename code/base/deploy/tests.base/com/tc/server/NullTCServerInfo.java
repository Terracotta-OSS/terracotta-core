/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server;

import com.tc.config.schema.L2Info;
import com.tc.config.schema.ServerGroupInfo;
import com.tc.config.schema.setup.TopologyReloadStatus;
import com.tc.management.AbstractTerracottaMBean;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.statistics.StatisticData;

import java.util.Map;

import javax.management.NotCompliantMBeanException;

public class NullTCServerInfo extends AbstractTerracottaMBean implements TCServerInfoMBean {

  public NullTCServerInfo() throws NotCompliantMBeanException {
    super(TCServerInfoMBean.class, false);
  }

  public void reset() {
    // nothing to reset
  }

  public long getActivateTime() {
    return 0;
  }

  public String getBuildID() {
    return "";
  }

  public String getCopyright() {
    return "";
  }

  public String getDescriptionOfCapabilities() {
    return "";
  }

  public L2Info[] getL2Info() {
    return null;
  }

  public int getDSOListenPort() {
    return 0;
  }

  public int getDSOGroupPort() {
    return 0;
  }

  public long getStartTime() {
    return 0;
  }

  public String getVersion() {
    return "";
  }

  public String getPatchLevel() {
    return "";
  }

  public String getPatchVersion() {
    return "";
  }

  public String getPatchBuildID() {
    return "";
  }

  public boolean isActive() {
    return false;
  }

  public boolean isStarted() {
    return false;
  }

  public boolean isShutdownable() {
    return false;
  }

  public void shutdown() {
    //
  }

  public void stop() {
    //
  }

  public String getHealthStatus() {
    return "";
  }

  public boolean isInStartState() {
    return false;
  }

  public boolean isPassiveStandby() {
    return false;
  }

  public boolean isPassiveUninitialized() {
    return false;
  }

  public void startBeanShell(int port) {
    //
  }

  public String[] getCpuStatNames() {
    return null;
  }

  public Map getStatistics() {
    return null;
  }

  public long getMaxMemory() {
    return 0;
  }

  public long getUsedMemory() {
    return 0;
  }

  public StatisticData[] getCpuUsage() {
    return null;
  }

  public byte[] takeCompressedThreadDump(long requestMillis) {
    return null;
  }

  public String getFailoverMode() {
    return null;
  }

  public String getPersistenceMode() {
    return null;
  }

  public String getEnvironment() {
    return null;
  }

  public String getConfig() {
    return null;
  }

  public boolean isPatched() {
    return false;
  }

  public String getState() {
    return null;
  }

  public boolean getFaultDebug() {
    return false;
  }

  public void setFaultDebug(boolean faultDebug) {
    /**/
  }

  public boolean getRequestDebug() {
    return false;
  }

  public void setRequestDebug(boolean requestDebug) {
    /**/
  }

  public boolean getFlushDebug() {
    return false;
  }

  public void setFlushDebug(boolean flushDebug) {
    /**/
  }

  public boolean getBroadcastDebug() {
    return false;
  }

  public void setBroadcastDebug(boolean broadcastDebug) {
    /**/
  }

  public boolean getCommitDebug() {
    return false;
  }

  public void setCommitDebug(boolean commitDebug) {
    /**/
  }

  public int getGarbageCollectionInterval() {
    return 0;
  }

  public boolean isGarbageCollectionEnabled() {
    return false;
  }

  public ServerGroupInfo[] getServerGroupInfo() {
    return null;
  }

  public String getMavenArtifactsVersion() {
    return null;
  }

  public void gc() {
    /**/
  }

  public boolean isVerboseGC() {
    return false;
  }

  public void setVerboseGC(boolean verboseGC) {
    /**/
  }

  public TopologyReloadStatus reloadConfiguration() {
    return null;
  }

  public boolean isEnterprise() {
    return false;
  }

  public String getTCProperties() {
    return null;
  }

  public String[] getProcessArguments() {
    return null;
  }
}
