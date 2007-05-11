/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import org.apache.commons.lang.StringUtils;

import com.tc.config.schema.L2ConfigForL1.L2Data;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.dynamic.DerivedConfigItem;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.net.core.ConnectionInfo;
import com.tc.util.Assert;
import com.tc.util.stringification.OurStringBuilder;

/**
 * Returns a {@link ConnectionInfo} array from the L2 data.
 */
public class ConnectionInfoConfigItem extends DerivedConfigItem {
  static TCLogger consoleLogger = CustomerLogging.getConsoleLogger();
  
  public ConnectionInfoConfigItem(ConfigItem l2DataConfigItem) {
    super(new ConfigItem[] { l2DataConfigItem });
  }

  protected Object createValueFrom(ConfigItem[] fromWhich) {
    ConnectionInfo[] out;

    String serversProperty = System.getProperty("tc.servers");
    if (serversProperty != null && (serversProperty = serversProperty.trim()) != null && serversProperty.length() > 0) {
      consoleLogger.info("tc.servers: " + serversProperty);
      
      String[] serverDescs = StringUtils.split(serversProperty, ",");
      int count = serverDescs.length;

      out = new ConnectionInfo[count];
      for (int i = 0; i < count; i++) {
        String[] serverDesc = StringUtils.split(serverDescs[i], ":");
        Assert.eval(serverDesc.length == 2);
        String host = serverDesc[0];
        int dsoPort = 0;
        
        try {
          dsoPort = Integer.parseInt(serverDesc[1]);
        } catch(NumberFormatException nfe) {
          consoleLogger.fatal("Cannot parse port for tc.servers element '"+serverDescs[i]+"'");
          System.exit(-1);
        }

        out[i] = new ConnectionInfo(host, dsoPort);
      }
    } else {
      Assert.eval(fromWhich.length == 1);

      L2Data[] l2Data = (L2Data[]) fromWhich[0].getObject();
      out = new ConnectionInfo[l2Data.length];

      for (int i = 0; i < out.length; ++i) {
        out[i] = new ConnectionInfo(l2Data[i].host(), l2Data[i].dsoPort());
      }
    }

    return out;
  }

  public String toString() {
    return new OurStringBuilder(this, OurStringBuilder.COMPACT_STYLE).appendSuper(super.toString()).toString();
  }
}