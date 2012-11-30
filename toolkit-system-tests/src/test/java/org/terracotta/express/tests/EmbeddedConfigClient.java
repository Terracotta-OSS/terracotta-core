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

public class EmbeddedConfigClient extends ClientBase {

  public EmbeddedConfigClient(String[] args) {
    super(args);
  }

  public static void main(String[] args) {
    new EmbeddedConfigClient(args).run();
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    String tcConfig = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">\n" +
      "<servers>\n" +
        "<server host=\"localhost\" name=\"testserver0\">\n" +
          "<dso-port>DSO_PORT</dso-port>\n" +
        "</server>\n" +
        "<mirror-groups>\n" +
          "<mirror-group group-name=\"testGroup0\">\n" +
          "<members>\n" +
              "<member>testserver0</member>\n" +
            "</members>\n" +
          "</mirror-group>\n" +
      "</mirror-groups>\n" +
      "</servers>\n" +
    "</tc:tc-config>";

    String dsoPort = getTerracottaUrl().split(":")[1];
    tcConfig = tcConfig.replace("DSO_PORT", dsoPort);

    TerracottaClientConfig config = new TerracottaClientConfigParams().tcConfigSnippetOrUrl(tcConfig).isUrl(false)
        .newTerracottaClientConfig();
    TerracottaInternalClient client1 = TerracottaInternalClientStaticFactory
        .getOrCreateTerracottaInternalClient(config);
    client1.init();

    // no assertion here, client1 should start correctly without problem

    client1.shutdown();
  }
}
