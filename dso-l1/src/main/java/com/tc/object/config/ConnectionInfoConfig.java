/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import org.apache.commons.lang.StringUtils;

import com.tc.config.schema.L2ConfigForL1.L2Data;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.net.core.ConnectionInfo;
import com.tc.util.stringification.OurStringBuilder;

/**
 * Returns a {@link ConnectionInfo} array from the L2 data.
 */
public class ConnectionInfoConfig {
  static TCLogger consoleLogger = CustomerLogging.getConsoleLogger();
  private final ConnectionInfo[] connectionInfos;

  public ConnectionInfoConfig(L2Data[] l2sData) {
    this.connectionInfos = createValueFrom(l2sData);
  }

  private ConnectionInfo[] createValueFrom(L2Data[] l2sData) {
    ConnectionInfo[] out;

    String serversProperty = System.getProperty("tc.server");
    if (serversProperty != null && (serversProperty = serversProperty.trim()) != null && serversProperty.length() > 0) {
      consoleLogger.info("tc.server: " + serversProperty);

      String[] serverDescs = StringUtils.split(serversProperty, ",");
      int count = serverDescs.length;

      out = new ConnectionInfo[count];
      for (int i = 0; i < count; i++) {
        String[] serverDesc = StringUtils.split(serverDescs[i], ":");
        String host = serverDesc.length > 0 ? serverDesc[0] : "localhost";
        int dsoPort = 9510;

        if (serverDesc.length == 2) {
          try {
            dsoPort = Integer.parseInt(serverDesc[1]);
          } catch (NumberFormatException nfe) {
            consoleLogger.warn("Cannot parse port for tc.server element '" + serverDescs[i]
                               + "'; Using default of 9510.");
          }
        }

        out[i] = new ConnectionInfo(host, dsoPort);
      }
    } else {
      out = new ConnectionInfo[l2sData.length];

      for (int i = 0; i < out.length; ++i) {
        out[i] = new ConnectionInfo(l2sData[i].host(), l2sData[i].dsoPort(), l2sData[i].getGroupId(), l2sData[i].getGroupName());
      }
    }

    return out;
  }
  
  public ConnectionInfo[] getConnectionInfos(){
    return this.connectionInfos;
  }

  public String toString() {
    StringBuilder l2sDataString = new StringBuilder();
    for(int i = 0; i < this.connectionInfos.length; i++){
      l2sDataString.append(this.connectionInfos[i].toString());
    }
    return new OurStringBuilder(this, OurStringBuilder.COMPACT_STYLE).appendSuper(l2sDataString.toString()).toString();
  }
}