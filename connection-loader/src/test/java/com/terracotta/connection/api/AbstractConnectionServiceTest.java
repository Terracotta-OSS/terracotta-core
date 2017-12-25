package com.terracotta.connection.api;

import com.terracotta.connection.client.TerracottaClientStripeConnectionConfig;
import org.junit.Test;

import java.net.URI;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;


public class AbstractConnectionServiceTest {

  private final AbstractConnectionService connectionService = new AbstractConnectionService("terracotta") {};

  @Test
  public void testURIHandling() {
    assertThat(connectionService.handlesURI(URI.create("terracotta://localhost:4000")), is(true));
    assertThat(connectionService.handlesURI(URI.create("diagnostic://localhost:4000")), is(false));
    assertThat(connectionService.handlesURI(URI.create("blah://localhost:4000")), is(false));
  }

  @Test
  public void testURIParsingWithSchemeMismatch() throws Exception {
    try {
      connectionService.connect(URI.create("diagnostic://localhost:4000,localhost:5000"), null);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), startsWith("Unknown URI"));
    }
  }

  @Test
  public void testURIParsingWithMalformedPort() throws Exception {
    try {
      connectionService.parseURI(URI.create("terracotta://localhost:4000,localhost:dd45"));
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), startsWith("Unable to parse uri"));
    }
  }

  @Test
  public void testURIParsing() throws Exception {
    URI uri = URI.create("diagnostic://localhost:4000,localhost:5000");
    TerracottaClientStripeConnectionConfig stripeConnectionConfig = connectionService.parseURI(uri);
    assertThat(stripeConnectionConfig.getStripeMemberUris(), hasItems("localhost:4000", "localhost:5000"));
  }
}