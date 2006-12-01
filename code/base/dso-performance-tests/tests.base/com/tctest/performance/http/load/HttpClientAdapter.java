/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.http.load;

import org.apache.commons.httpclient.HttpState;

public class HttpClientAdapter {
  private final HttpState session;
  private String           host;

  public HttpClientAdapter(HttpState session, String host) {
    this.session = session;
    this.host = host;
  }

  HttpState getSession() {
    return session;
  }

  public synchronized void setHost(String host) {
    this.host = host;
  }

  public synchronized String getHost() {
    return host;
  }
}