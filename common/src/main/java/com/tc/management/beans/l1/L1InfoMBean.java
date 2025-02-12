/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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
