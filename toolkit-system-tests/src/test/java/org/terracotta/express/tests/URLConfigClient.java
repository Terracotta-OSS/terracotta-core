/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;

import com.tc.util.Assert;
import com.terracotta.toolkit.client.TerracottaClientConfig;
import com.terracotta.toolkit.client.TerracottaClientConfigParams;
import com.terracotta.toolkit.express.TerracottaInternalClient;
import com.terracotta.toolkit.express.TerracottaInternalClientStaticFactory;

public class URLConfigClient extends ClientBase {

  public URLConfigClient(String[] args) {
    super(args);
  }

  public static void main(String[] args) {
    new URLConfigClient(args).run();
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    String yoyo = "YO-YO-YO-YO";
    String yoyodb = "${" + yoyo + "}";
    String configUrl = getTerracottaUrl();
    System.setProperty(yoyo, configUrl);

    TerracottaClientConfig config = new TerracottaClientConfigParams().tcConfigSnippetOrUrl(configUrl).isUrl(true)
        .newTerracottaClientConfig();
    TerracottaInternalClient client1 = TerracottaInternalClientStaticFactory
        .getOrCreateTerracottaInternalClient(config);
    client1.init();

    // clusteringToolkit client is shared with client1
    Assert.assertFalse(client1.isDedicatedClient());

    config = new TerracottaClientConfigParams().tcConfigSnippetOrUrl(yoyodb).isUrl(true).newTerracottaClientConfig();
    TerracottaInternalClient client2 = TerracottaInternalClientStaticFactory
        .getOrCreateTerracottaInternalClient(config);
    client2.init();

    Assert.assertFalse(client2.isDedicatedClient());

    // these two clients should be same because now we share L1s with same URL
    if (client1 != client2) { throw new AssertionError(); }

    System.setProperty(yoyo, configUrl + ",localhost:1234");
    config = new TerracottaClientConfigParams().tcConfigSnippetOrUrl(yoyodb).isUrl(true).newTerracottaClientConfig();
    TerracottaInternalClient client3 = TerracottaInternalClientStaticFactory
        .getOrCreateTerracottaInternalClient(config);
    client3.init();

    Assert.assertFalse(client2.isDedicatedClient());

    if (client3 == client1) { throw new AssertionError(); }

    client1.shutdown();
    Assert.assertFalse(client1.isShutdown());
    Assert.assertFalse(client2.isShutdown());

    client2.shutdown();
    // still shared by clusteringToolkit client
    Assert.assertFalse(client1.isShutdown());
    Assert.assertFalse(client2.isShutdown());

    getClusteringToolkit().shutdown();
    Assert.assertTrue(client1.isShutdown());
    Assert.assertTrue(client2.isShutdown());
    client1 = null;
    client2 = null;

    client3.shutdown();
    Assert.assertTrue(client3.isShutdown());
    client3 = null;
  }
}
