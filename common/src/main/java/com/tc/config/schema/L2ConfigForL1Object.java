/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import org.terracotta.config.Server;
import org.terracotta.config.Servers;

import com.tc.config.schema.context.ConfigContext;
import com.tc.net.TCSocketAddress;
import com.tc.util.Assert;

/**
 * The standard implementation of {@link L2ConfigForL1}.
 */
public class L2ConfigForL1Object implements L2ConfigForL1 {

  private final Servers l2sContext;

  private final L2Data[]                   l2sData;
  private final L2Data[][]                       l2DataByGroup;

  public L2ConfigForL1Object(Servers l2sContext, ConfigContext systemContext) {
    this(l2sContext, systemContext, null);
  }

  public L2ConfigForL1Object(Servers l2sContext, ConfigContext systemContext, int[] tsaPorts) {
    Assert.assertNotNull(l2sContext);

    this.l2sContext = l2sContext;


    Servers servers = this.l2sContext;
    boolean securityEnabled = servers.isSecure();
    Server[] l2Array = getAllServers(servers);
    this.l2sData = new L2Data[l2Array.length];
    for (int i = 0; i < l2Array.length; i++) {
      Server l2 = l2Array[i];
      String host = l2.getTsaPort().getBind();
      if (TCSocketAddress.WILDCARD_IP.equals(host)) {
        host = l2.getHost();
      }
      this.l2sData[i] = new L2Data(host, l2.getTsaPort().getValue(), securityEnabled);
    }
    this.l2DataByGroup = new L2Data[][] { this.l2sData };
  }

  private static Server[] getAllServers(Servers servers) {
    return servers.getServer().toArray(new Server[servers.getServer().size()]);
  }

  @Override
  public L2Data[] l2Data() {
    return this.l2sData;
  }

  @Override
  public synchronized L2Data[][] getL2DataByGroup() {
    return this.l2DataByGroup;
  }
}
