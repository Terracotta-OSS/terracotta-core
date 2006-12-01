/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.http.load;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;

/**
 * This is a work item that's associated with a specific session
 */

public class SessionWorkItem extends ExpiringWorkItem {

  private final String            url;
  private final HttpMethod        method;
  private final boolean           gatherStatistic;
  private final HttpClientAdapter clientAdapter;

  public SessionWorkItem(HttpClientAdapter clientAdapter, String urlPart, boolean gatherStatistic, long expire) {
    super(expire);
    this.clientAdapter = clientAdapter;
    this.url = "http://" + clientAdapter.getHost() + urlPart;
    this.method = new GetMethod(url);
    this.gatherStatistic = gatherStatistic;
  }

  public void executeImpl(HttpClient httpClient, StatsCollector collector) throws IOException {
    HttpState state = clientAdapter.getSession();
    httpClient.setState(state);

    long startTime = System.currentTimeMillis();
    try {
      int statusCode = httpClient.executeMethod(method);
      if (gatherStatistic) {
        collector.addStat(new ResponseStatistic(startTime, System.currentTimeMillis(), url, statusCode));
      }
      consumeResponse(method);
    } finally {
      method.releaseConnection();
    }
  }

}