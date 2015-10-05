/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.connection;

import com.terracotta.connection.client.TerracottaClientConfigParams;

import java.util.Set;


public class TerracottaInternalClientFactoryImpl implements TerracottaInternalClientFactory {
  public static final String  SECRET_PROVIDER = "com.terracotta.express.SecretProvider";

  // public nullary constructor needed as entry point from SPI
  public TerracottaInternalClientFactoryImpl() {
    testForWrongTcconfig();

    System.setProperty("tc.active", "true");
    System.setProperty("tc.dso.globalmode", "false");
  }

  @Override
  public TerracottaInternalClient createL1Client(TerracottaClientConfigParams config) {
    String initialTcConfigUrl = config.getTcConfigUrl();
    String expandedTcConfigUrl = URLConfigUtil.translateSystemProperties(initialTcConfigUrl);
    return createClient(expandedTcConfigUrl, config.getTunnelledMBeanDomains(), config.getProductId());
  }


  private TerracottaInternalClient createClient(String tcConfig, Set<String> tunneledMBeanDomains, String productId) {
    TerracottaInternalClient client = new TerracottaInternalClientImpl(tcConfig, tunneledMBeanDomains, productId);
    return client;
  }

  private static void testForWrongTcconfig() {
    String tcConfigValue = System.getProperty("tc.config");
    if (tcConfigValue != null) {
      //
      throw new RuntimeException("The Terracotta config file should not be set through -Dtc.config in this usage.");
    }
  }
}
