/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;

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
    TerracottaInternalClient client1 = TerracottaInternalClientStaticFactory.createTerracottaL1Client(config);

    config = new TerracottaClientConfigParams().tcConfigSnippetOrUrl(yoyodb).isUrl(true).newTerracottaClientConfig();
    TerracottaInternalClient client2 = TerracottaInternalClientStaticFactory.createTerracottaL1Client(config);

    // these two clients should not be same because now we do not share L1s with same URL
    if (client1 == client2) { throw new AssertionError(); }

    System.setProperty(yoyo, configUrl + ",localhost:1234");
    config = new TerracottaClientConfigParams().tcConfigSnippetOrUrl(yoyodb).isUrl(true).newTerracottaClientConfig();
    TerracottaInternalClient client3 = TerracottaInternalClientStaticFactory.createTerracottaL1Client(config);

    if (client3 == client1) { throw new AssertionError(); }

    client1.shutdown();
    client1 = null;
    client2.shutdown();
    client2 = null;
    client3.shutdown();
    client3 = null;

    TerracottaInternalClientStaticFactory.createTerracottaL1Client(new TerracottaClientConfigParams()
                                                                       .tcConfigSnippetOrUrl(yoyodb).isUrl(true)
                                                                       .newTerracottaClientConfig())
        .shutdown();
  }
}
