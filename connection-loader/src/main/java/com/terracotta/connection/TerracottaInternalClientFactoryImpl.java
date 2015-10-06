/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.connection;

import com.terracotta.connection.client.TerracottaClientConfigParams;
import com.terracotta.connection.client.TerracottaClientStripeConnectionConfig;

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
    // Translate the URIs for the stripe with the system properties and use that to create the stripe config.
    TerracottaClientStripeConnectionConfig stripeConnectionConfig = new TerracottaClientStripeConnectionConfig();
    for (String memberUri : config.getStripeMemberUris()) {
      String expandedMemberUri = URLConfigUtil.translateSystemProperties(memberUri);
      stripeConnectionConfig.addStripeMemberUri(expandedMemberUri);
    }
    return createClient(stripeConnectionConfig, config.getTunnelledMBeanDomains(), config.getProductId());
  }


  private TerracottaInternalClient createClient(TerracottaClientStripeConnectionConfig stripeConnectionConfig, Set<String> tunneledMBeanDomains, String productId) {
    TerracottaInternalClient client = new TerracottaInternalClientImpl(stripeConnectionConfig, tunneledMBeanDomains, productId);
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
