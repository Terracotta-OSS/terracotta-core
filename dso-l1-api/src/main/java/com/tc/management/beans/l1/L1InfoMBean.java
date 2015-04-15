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

  void startBeanShell(int port);

  String getEnvironment();

  String getConfig();

  Map getStatistics();

  long getUsedMemory();

  long getMaxMemory();

  boolean isVerboseGC();

  void setVerboseGC(boolean verboseGC);

  void gc();

  String getTCProperties();

  String[] getProcessArguments();
}
