/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.http.load;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;

public class OneHitWorkItem extends ExpiringWorkItem {

  private final String  url;
  private final boolean doStats;

  public OneHitWorkItem(final String host, final String url) {
    this(host, url, false, Long.MAX_VALUE);
  }

  public OneHitWorkItem(final String host, final String url, final boolean doStats, final long endtime) {
    super(endtime);
    this.url = "http://" + host + url;
    this.doStats = doStats;
  }

  public void executeImpl(HttpClient httpClient, StatsCollector collector) throws IOException {
    httpClient.setState(new HttpState()); // clean state (no session)

    final GetMethod method = new GetMethod(url);
    try {
      final long startTime = System.currentTimeMillis();
      final int statusCode = httpClient.executeMethod(method);
      final long endTime = System.currentTimeMillis();
      if (doStats) collector.addStat(new ResponseStatistic(startTime, endTime, url, statusCode));
      consumeResponse(method);
    } finally {
      method.releaseConnection();
    }
  }
}
