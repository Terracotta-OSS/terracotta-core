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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.config.schema.L2ConfigForL1.L2Data;
import com.tc.net.core.ConnectionInfo;

import java.util.Arrays;

/**
 * Returns a {@link ConnectionInfo} array from the L2 data.
 */
public class ConnectionInfoConfig {
  static Logger LOGGER = LoggerFactory.getLogger(ConnectionInfoConfig.class);
  private final ConnectionInfo[] connectionInfos;

  public ConnectionInfoConfig(L2Data[] l2sData) {
    this.connectionInfos = createValueFrom(l2sData);
  }

  private ConnectionInfo[] createValueFrom(L2Data[] l2sData) {
    ConnectionInfo[] out;

    String serversProperty = System.getProperty("tc.server");
    if (serversProperty != null && (serversProperty = serversProperty.trim()) != null && serversProperty.length() > 0) {
      LOGGER.info("tc.server: " + serversProperty);

      String[] serverDescs = serversProperty.split(",");
      int count = serverDescs.length;

      out = new ConnectionInfo[count];
      for (int i = 0; i < count; i++) {
        String serverDesc = serverDescs[i];
        String host;
        int tsaPort = 9410;
        if (serverDesc.startsWith("[")) {
          if (!serverDesc.contains("]")) {
            throw new IllegalArgumentException(String.format("A tc.server element contains an invalid host '%s'. "
                    + "IPv6 address literals must be enclosed in '[' and ']' according to RFC 2732", serverDesc));
          }
          int end = serverDesc.indexOf("]");
          host = serverDesc.substring(1, end);
          String remainder = serverDesc.substring(end + 1);
          if (!remainder.isEmpty() && remainder.charAt(0) == ':') {
            String portString = remainder.substring(1);
            try {
              tsaPort = Integer.parseInt(portString);
            } catch (NumberFormatException nfe) {
              LOGGER.info("Cannot parse port for tc.server element '" + portString
                          + "'; Using default of 9410.");
            }
          }
        } else {
          String[] parts = serverDesc.split(":");

          host = parts.length > 0 ? parts[0] : "localhost";
          if (parts.length == 2) {
            try {
              tsaPort = Integer.parseInt(parts[1]);
            } catch (NumberFormatException nfe) {
              LOGGER.info("Cannot parse port for tc.server element '" + serverDescs[i]
                          + "'; Using default of 9410.");
            }
          }
        }

        out[i] = new ConnectionInfo(host, tsaPort);
      }
    } else {
      out = new ConnectionInfo[l2sData.length];

      for (int i = 0; i < out.length; ++i) {
        out[i] = new ConnectionInfo(l2sData[i].host(), l2sData[i].tsaPort(), l2sData[i].getGroupId());
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
