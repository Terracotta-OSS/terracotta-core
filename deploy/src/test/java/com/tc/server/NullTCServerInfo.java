/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.server;

import com.tc.config.schema.L2Info;
import com.tc.config.schema.ServerGroupInfo;
import com.tc.management.AbstractTerracottaMBean;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.objectserver.api.BackupManager;
import com.tc.objectserver.api.ResourceManager;

import java.util.Map;

import javax.management.NotCompliantMBeanException;

public class NullTCServerInfo extends AbstractTerracottaMBean implements TCServerInfoMBean {

  public NullTCServerInfo() throws NotCompliantMBeanException {
    super(TCServerInfoMBean.class, false);
  }

  @Override
  public void reset() {
    // nothing to reset
  }

  @Override
  public long getActivateTime() {
    return 0;
  }

  @Override
  public String getBuildID() {
    return "";
  }

  @Override
  public String getCopyright() {
    return "";
  }

  @Override
  public String getDescriptionOfCapabilities() {
    return "";
  }

  @Override
  public L2Info[] getL2Info() {
    return null;
  }

  @Override
  public String getL2Identifier() {
    return null;
  }

  @Override
  public int getTSAListenPort() {
    return 0;
  }

  @Override
  public int getTSAGroupPort() {
    return 0;
  }

  @Override
  public int getManagementPort() {
    return 0;
  }

  @Override
  public long getStartTime() {
    return 0;
  }

  @Override
  public String getVersion() {
    return "";
  }

  @Override
  public String getPatchLevel() {
    return "";
  }

  @Override
  public String getPatchVersion() {
    return "";
  }

  @Override
  public String getPatchBuildID() {
    return "";
  }

  @Override
  public boolean isActive() {
    return false;
  }

  @Override
  public boolean isStarted() {
    return false;
  }

  @Override
  public boolean isShutdownable() {
    return false;
  }

  @Override
  public void shutdown() {
    //
  }

  @Override
  public void stop() {
    //
  }

  @Override
  public String getHealthStatus() {
    return "";
  }

  @Override
  public boolean isPassiveStandby() {
    return false;
  }

  @Override
  public boolean isPassiveUninitialized() {
    return false;
  }

  @Override
  public boolean isRecovering() {
    return false;
  }

  @Override
  public Map getStatistics() {
    return null;
  }

  @Override
  public long getMaxMemory() {
    return 0;
  }

  @Override
  public long getUsedMemory() {
    return 0;
  }

  @Override
  public byte[] takeCompressedThreadDump(long requestMillis) {
    return null;
  }

  @Override
  public boolean getRestartable() {
    return false;
  }

  @Override
  public String getEnvironment() {
    return null;
  }

  @Override
  public String getConfig() {
    return null;
  }

  @Override
  public boolean isPatched() {
    return false;
  }

  @Override
  public String getState() {
    return null;
  }

  @Override
  public boolean getRequestDebug() {
    return false;
  }

  @Override
  public void setRequestDebug(boolean requestDebug) {
    /**/
  }

  @Override
  public boolean getBroadcastDebug() {
    return false;
  }

  @Override
  public void setBroadcastDebug(boolean broadcastDebug) {
    /**/
  }

  @Override
  public boolean getCommitDebug() {
    return false;
  }

  @Override
  public void setCommitDebug(boolean commitDebug) {
    /**/
  }

  @Override
  public int getGarbageCollectionInterval() {
    return 0;
  }

  @Override
  public boolean isGarbageCollectionEnabled() {
    return false;
  }

  @Override
  public ServerGroupInfo[] getServerGroupInfo() {
    return null;
  }

  @Override
  public String getMavenArtifactsVersion() {
    return null;
  }

  @Override
  public void gc() {
    /**/
  }

  @Override
  public boolean isVerboseGC() {
    return false;
  }

  @Override
  public void setVerboseGC(boolean verboseGC) {
    /**/
  }

  @Override
  public boolean isEnterprise() {
    return false;
  }

  @Override
  public String getTCProperties() {
    return null;
  }

  @Override
  public String[] getProcessArguments() {
    return null;
  }

  @Override
  public boolean isSecure() {
    return false;
  }

  @Override
  public String getSecurityServiceLocation() {
    return null;
  }

  @Override
  public Integer getSecurityServiceTimeout() {
    return null;
  }

  @Override
  public String getSecurityHostname() {
    return null;
  }

  @Override
  public String getIntraL2Username() {
    return null;
  }

  @Override
  public void backup(final String name) {
    //
  }

  @Override
  public String getRunningBackup() {
    return null;
  }

  @Override
  public String getBackupStatus(final String name) {
    return BackupManager.BackupStatus.UNKNOWN.toString();
  }

  @Override
  public String getBackupFailureReason(String name) {
    return null;
  }

  @Override
  public String getResourceState() {
    return ResourceManager.State.NORMAL.name();
  }

  @Override
  public Map<String, String> getBackupStatuses() {
    return null;
  }

  @Override
  public boolean isLegacyProductionModeEnabled() {
    return false;
  }
}
