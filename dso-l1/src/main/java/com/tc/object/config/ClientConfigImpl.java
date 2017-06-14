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
package com.tc.object.config;


import com.tc.config.schema.CommonL1Config;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.core.SecurityInfo;
import com.tc.properties.ReconnectConfig;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.security.PwProvider;
import com.tc.util.ProductInfo;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.io.ServerURL;
import com.tc.util.version.Version;
import com.tc.util.version.VersionCompatibility;

import java.io.IOException;
import java.net.MalformedURLException;

public class ClientConfigImpl implements ClientConfig {

  private static final TCLogger logger = TCLogging.getLogger(ClientConfigImpl.class);

  private final L1ConfigurationSetupManager                      configSetupManager;
  private final ReconnectConfig                                        l1ReconnectConfig           = null;
  private static final long                                      CONFIGURATION_TOTAL_TIMEOUT = TCPropertiesImpl
                                                                                                 .getProperties()
                                                                                                 .getLong(TCPropertiesConsts.TC_CONFIG_TOTAL_TIMEOUT);

  public ClientConfigImpl(boolean initializedModulesOnlyOnce, L1ConfigurationSetupManager configSetupManager) {
    this(configSetupManager);
  }

  public ClientConfigImpl(L1ConfigurationSetupManager configSetupManager) {
    this.configSetupManager = configSetupManager;
  }

  @Override
  public String rawConfigText() {
    return configSetupManager.rawConfigText();
  }

  @Override
  public CommonL1Config getCommonL1Config() {
    return configSetupManager.commonL1Config();
  }

  @Override
  public SecurityInfo getSecurityInfo() {
    return configSetupManager.getSecurityInfo();
  }

  @Override
  public String toString() {
    return "<ClientConfigImpl: " + configSetupManager + ">";
  }

  @Override
  public void validateClientServerCompatibility(PwProvider pwProvider, SecurityInfo securityInfo)
      throws ConfigurationSetupException {
    PreparedComponentsFromL2Connection connectionComponents = new PreparedComponentsFromL2Connection(configSetupManager);
    ConnectionInfoConfig connectionInfoItem = connectionComponents.createConnectionInfoConfigItem();
      ConnectionInfo[] connectionInfo = connectionInfoItem.getConnectionInfos();
      boolean foundCompatibleActive = false;
      boolean activeDown = false;
      int serverNumberInStripe = 0;
      long startTime = System.currentTimeMillis();
      long endTime = System.currentTimeMillis();
      // keep looping till we find version of an active server
      // or the timeout occurs
      while ((endTime - startTime) < CONFIGURATION_TOTAL_TIMEOUT) {

      ConnectionInfo connectionIn = new ConnectionInfo(connectionInfo[serverNumberInStripe].getHostname(),
                                                       connectionInfo[serverNumberInStripe].getPort(),
                                                       serverNumberInStripe,
                                                       connectionInfo[serverNumberInStripe].getSecurityInfo());

        ServerURL serverUrl = null;
        try {
          String[] host = this.configSetupManager.source().split(":");
          serverUrl = new ServerURL(host[0], Integer.parseInt(host[1]), "/version",
                                    connectionIn.getSecurityInfo());
        } catch (MalformedURLException e) {
          throw new ConfigurationSetupException("Error while trying to verify Client-Server version Compatibility ");
        }

        String strServerVersion = null;
        try {
          strServerVersion = serverUrl.getHeaderField("Version", pwProvider, true);
          activeDown = false;
          logger.info("Server: " + serverUrl + " returned server version = " + strServerVersion);
        } catch (IOException e) {
          // server that we pinged was not up
          // we should try other servers in stripe
          activeDown = true;
          logger.info("Server seems to be down.." + serverUrl + ", retrying next available in stripe");
        }
        if (strServerVersion == null) {
          if (serverNumberInStripe == (connectionInfo.length - 1)) {
            if (activeDown) {
              // active was down and we have reached the end of connectionInfo Array
              // so we need to start checking from 0th index again
              ThreadUtil.reallySleep(500); // sleep for 1 sec before trying again
              serverNumberInStripe = 0;
            } else {
              // active was not down and we have reached end of array
              // we didn't find any compatible active
              foundCompatibleActive = false;
              break;
            }
          } else {
            // we found serverNumberInStripe = null
            // but there are some server left in stripe we should try to get version from them
            serverNumberInStripe++;
          }
          endTime = System.currentTimeMillis();
          continue;
        } else {
          Version serverVersion = new Version(strServerVersion);
          foundCompatibleActive = checkServerClientVersion(serverVersion, serverUrl);
          break;
        }
      }
      if ((endTime - startTime) > CONFIGURATION_TOTAL_TIMEOUT) { throw new ConfigurationSetupException(
                                                                                                       "Timeout occured while trying to get Server Version, No Active server Found for : "
                                                                                                           + CONFIGURATION_TOTAL_TIMEOUT); }
      if (!foundCompatibleActive) {
        if (activeDown) {
          throw new IllegalStateException(
                                          "At least one of the stripes is down, couldn't get the server version for compatibility check!");
        } else {
          throw new IllegalStateException("client Server Version mismatch occured: client version : "
                                          + getClientVersion()
                                          + " is not compatible with a server of Terracotta version: 4.0 or before");
        }
      }
  }

  private boolean checkServerClientVersion(Version serverVersion, ServerURL serverUrl) {
    Version clientVersion = getClientVersion();
    if (!new VersionCompatibility().isCompatibleClientServer(clientVersion, serverVersion)) {
      throw new IllegalStateException("Client-Server versions are incompatible: client version=" + clientVersion
                                      + ", serverVersion=" + serverVersion);
    } else {
      logger.debug("Found Compatible active Server = " + serverUrl);
      return true;
    }
  }

  private Version getClientVersion() {
    return new Version(ProductInfo.getInstance().version());
  }


  private void setupL1ReconnectProperties(PwProvider pwProvider) throws ConfigurationSetupException {

  }

  @Override
  public synchronized ReconnectConfig getL1ReconnectProperties(PwProvider securityManager)
      throws ConfigurationSetupException {
    if (l1ReconnectConfig == null) {
      setupL1ReconnectProperties(securityManager);
    }
    return l1ReconnectConfig;
  }
}
