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
package com.tc.object.config.schema;

import com.tc.config.schema.Config;
import com.terracottatech.config.BindPort;
import com.terracottatech.config.DataStorage;
import com.terracottatech.config.DataStorageOffheap;
import com.terracottatech.config.GarbageCollection;
import com.terracottatech.config.Restartable;
import com.terracottatech.config.Security;

/**
 * Represents all configuration read by the DSO L2 and which is independent of application.
 */
public interface L2DSOConfig extends Config {

  public static final String OBJECTDB_DIRNAME                      = "objectdb";
  public static final String DIRTY_OBJECTDB_BACKUP_DIRNAME         = "dirty-objectdb-backup";
  public static final String DIRTY_OBJECTDB_BACKUP_PREFIX          = "dirty-objectdb-";

  GarbageCollection garbageCollection();

  BindPort tsaPort();

  BindPort tsaGroupPort();

  BindPort managementPort();

  String host();

  String serverName();

  int clientReconnectWindow();

  String bind();

  Security securityConfig();

  DataStorageOffheap getOffheap();

  DataStorage getDataStorage();

  Restartable getRestartable();

  boolean isJmxEnabled();

  void setJmxEnabled(boolean b);
}
