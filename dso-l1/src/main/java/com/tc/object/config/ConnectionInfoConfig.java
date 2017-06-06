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

import com.tc.config.schema.L2ConfigForL1.L2Data;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.core.SecurityInfo;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;

/**
 * Returns a {@link ConnectionInfo} array from the L2 data.
 */
public class ConnectionInfoConfig {
  static TCLogger consoleLogger = TCLogging.getConsoleLogger();
  private final ConnectionInfo[] connectionInfos;

  public ConnectionInfoConfig(L2Data[] l2sData) {
    this(l2sData, new SecurityInfo());
  }

  public ConnectionInfoConfig(L2Data[] l2sData, SecurityInfo securityInfo) {
    this.connectionInfos = createValueFrom(l2sData, securityInfo);
  }

  private ConnectionInfo[] createValueFrom(L2Data[] l2sData, SecurityInfo securityInfo) {
    ConnectionInfo[] out;

    String serversProperty = System.getProperty("tc.server");
    if (serversProperty != null && (serversProperty = serversProperty.trim()) != null && serversProperty.length() > 0) {
      consoleLogger.info("tc.server: " + serversProperty);

      String[] serverDescs = serversProperty.split(",");
      int count = serverDescs.length;

      out = new ConnectionInfo[count];
      for (int i = 0; i < count; i++) {
        String[] serverDesc = serverDescs[i].split(":");
        String host = serverDesc.length > 0 ? serverDesc[0] : "localhost";
        int tsaPort = 9410;

        if (serverDesc.length == 2) {
          try {
            tsaPort = Integer.parseInt(serverDesc[1]);
          } catch (NumberFormatException nfe) {
            consoleLogger.warn("Cannot parse port for tc.server element '" + serverDescs[i]
                               + "'; Using default of 9410.");
          }
        }

        boolean secure = false;
        String urlUsername = null;
        int userSeparatorIndex = host.indexOf('@');
        if (userSeparatorIndex > -1) {
          secure = true;
          urlUsername = host.substring(0, userSeparatorIndex);
          try {
            urlUsername = URLDecoder.decode(urlUsername, "UTF-8");
          } catch (UnsupportedEncodingException uee) {
            // cannot happen
          }
          host = host.substring(userSeparatorIndex + 1);
        }

        out[i] = new ConnectionInfo(host, tsaPort, new SecurityInfo(secure, urlUsername));
      }
    } else {
      out = new ConnectionInfo[l2sData.length];

      for (int i = 0; i < out.length; ++i) {
        out[i] = new ConnectionInfo(l2sData[i].host(), l2sData[i].tsaPort(), l2sData[i].getGroupId(), securityInfo);
      }
    }

    return out;
  }

  public ConnectionInfo[] getConnectionInfos(){
    return this.connectionInfos;
  }

  @Override
  public String toString() {
    return "ConnectionInfoConfig [connectionInfos=" + Arrays.toString(connectionInfos) + "]";
  }  
}
