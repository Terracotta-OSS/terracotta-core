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
package com.terracotta.connection;

import com.terracotta.connection.client.TerracottaClientConfigParams;
import com.terracotta.connection.client.TerracottaClientStripeConnectionConfig;

import java.util.Properties;


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
    return createClient(stripeConnectionConfig, config.getProductId(), config.getGenericProperties());
  }


  private TerracottaInternalClient createClient(TerracottaClientStripeConnectionConfig stripeConnectionConfig, String productId, Properties props) {
    TerracottaInternalClient client = new TerracottaInternalClientImpl(stripeConnectionConfig, productId, props);
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
