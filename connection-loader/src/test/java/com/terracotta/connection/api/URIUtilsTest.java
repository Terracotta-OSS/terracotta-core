/*
 * Copyright (c) 2011-2017 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracotta.connection.api;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.net.URI;
import java.net.URISyntaxException;

public class URIUtilsTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void allowSingleServerURI() throws Exception {
    URI uri = new URI("terracotta://server:1234");
    URIUtils.validateTerracottaURI(uri);
  }

  @Test
  public void allowSingleServerURIWithSlash() throws Exception {
    URI uri = new URI("terracotta://server:1234/");
    URIUtils.validateTerracottaURI(uri);
  }

  @Test
  public void preventSingleServerURIWithPath() throws Exception {
    URI uri = new URI("terracotta://server:1234/path");
    expectedException.expect(URISyntaxException.class);
    URIUtils.validateTerracottaURI(uri);
  }

  @Test
  public void preventSingleServerURIWithEmptyQuery() throws Exception {
    URI uri = new URI("terracotta://server:1234?");
    expectedException.expect(URISyntaxException.class);
    URIUtils.validateTerracottaURI(uri);
  }

  @Test
  public void preventSingleServerURIWithQuery() throws Exception {
    URI uri = new URI("terracotta://server:1234?abc=def");
    expectedException.expect(URISyntaxException.class);
    URIUtils.validateTerracottaURI(uri);
  }

  @Test
  public void preventSingleServerURIWithFragment() throws Exception {
    URI uri = new URI("terracotta://server:1234#fragment");
    expectedException.expect(URISyntaxException.class);
    URIUtils.validateTerracottaURI(uri);
  }

  @Test
  public void allowMultiServerURI() throws Exception {
    URI uri = new URI("terracotta://server1:1234,server2:5678");
    URIUtils.validateTerracottaURI(uri);
  }

  @Test
  public void allowMultiServerURIWithSlash() throws Exception {
    URI uri = new URI("terracotta://server1:1234,server2:5678/");
    URIUtils.validateTerracottaURI(uri);
  }

  @Test
  public void preventMultiServerURIWithPath() throws Exception {
    URI uri = new URI("terracotta://server1:1234,server2:5678/path");
    expectedException.expect(URISyntaxException.class);
    URIUtils.validateTerracottaURI(uri);
  }
}
