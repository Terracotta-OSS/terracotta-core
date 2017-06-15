/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.object.config.schema;

import org.terracotta.config.BindPort;
import org.terracotta.config.Server;

import com.tc.config.schema.Config;


/**
 * Represents all configuration read by the DSO L2 and which is independent of application.
 */
public interface L2Config extends Config<Server> {

  public static final String OBJECTDB_DIRNAME                      = "objectdb";
  public static final String DIRTY_OBJECTDB_BACKUP_DIRNAME         = "dirty-objectdb-backup";
  public static final String DIRTY_OBJECTDB_BACKUP_PREFIX          = "dirty-objectdb-";

  BindPort tsaPort();

  BindPort tsaGroupPort();

  String host();

  String serverName();

  int clientReconnectWindow();

  String bind();

  boolean isJmxEnabled();

  void setJmxEnabled(boolean b);
}
