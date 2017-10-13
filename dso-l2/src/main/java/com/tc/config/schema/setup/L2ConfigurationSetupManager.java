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
package com.tc.config.schema.setup;

import com.tc.classloader.ServiceLocator;
import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.CommonL2Config;
import com.tc.object.config.schema.L2Config;
import com.tc.server.ServerConnectionValidator;

import java.io.InputStream;

/**
 * Knows how to set up configuration for L2.
 */
public interface L2ConfigurationSetupManager {
  String[] processArguments();

  CommonL2Config commonl2Config();

  L2Config dsoL2Config();

  ActiveServerGroupConfig getActiveServerGroupForThisL2();

  String describeSources();

  InputStream rawConfigFile();

  InputStream effectiveConfigFile();

  String[] allCurrentlyKnownServers();

  String getL2Identifier();

  CommonL2Config commonL2ConfigFor(String name) throws ConfigurationSetupException;

  L2Config dsoL2ConfigFor(String name) throws ConfigurationSetupException;

  TopologyReloadStatus reloadConfiguration(ServerConnectionValidator serverConnectionValidator)
      throws ConfigurationSetupException;
  
  ServiceLocator getServiceLocator();
}
