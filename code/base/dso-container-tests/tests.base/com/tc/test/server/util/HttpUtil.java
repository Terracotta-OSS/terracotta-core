/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.util;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;

/**
 * This utility is meant to be expanded. It delegates to the Apache Commons Http package.
 */
public final class HttpUtil {

  private static final int     DEFAULT_TIMEOUT  = 30 * 1000;
  private static final int     DEFAULT_MAX_CONN = 1000;

  private static final boolean DEBUG            = false;

  private HttpUtil() {
    // cannot instantiate
  }

  public static HttpClient createHttpClient() {
    HttpClient client = new HttpClient(new MultiThreadedHttpConnectionManager());
    client.getHttpConnectionManager().getParams().setConnectionTimeout(DEFAULT_TIMEOUT);
    client.getHttpConnectionManager().getParams().setMaxTotalConnections(DEFAULT_MAX_CONN);
    return client;
  }

  public static boolean getBoolean(URL url, HttpClient client) throws ConnectException, IOException {
    return Boolean.valueOf(getResponseBody(url, client)).booleanValue();
  }

  public static boolean[] getBooleanValues(URL url, HttpClient client) throws ConnectException, IOException {
    String responseBody = getResponseBody(url, client);
    String[] lines = responseBody.split("\n");
    boolean[] values = new boolean[lines.length];
    for (int i = 0; i < lines.length; i++) {
      values[i] = Boolean.valueOf(lines[i].trim()).booleanValue();
    }
    return values;
  }

  public static String getResponseBody(URL url, HttpClient client) throws ConnectException, IOException {
    Cookie[] cookies = client.getState().getCookies();
    for (int i = 0; i < cookies.length; i++) {
      debugPrint("localClient... cookie " + i + ": " + cookies[i].toString());
    }

    GetMethod get = new GetMethod(url.toString());

    // this disables the automatic request retry junk in HttpClient
    get.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, NoRetryHandler.INSTANCE);

    try {
      int status = client.executeMethod(get);
      if (status != HttpStatus.SC_OK) {
        // make formatter sane
        throw new ConnectException("The http client has encountered a status code other than ok for the url: " + url
                                   + " status: " + HttpStatus.getStatusText(status));
      }
      return get.getResponseBodyAsString().trim();
    } finally {
      get.releaseConnection();
    }
  }

  public static int getInt(URL url, HttpClient client) throws ConnectException, IOException {
    return Integer.valueOf(getResponseBody(url, client)).intValue();
  }

  public static int[] getIntValues(URL url, HttpClient client) throws ConnectException, IOException {
    String responseBody = getResponseBody(url, client);
    String[] lines = responseBody.split("\n");
    int[] values = new int[lines.length];
    for (int i = 0; i < lines.length; i++) {
      values[i] = Integer.valueOf(lines[i].trim()).intValue();
    }
    return values;
  }

  private static void debugPrint(String s) {
    if (DEBUG) {
      System.out.println("XXXXX " + s);
    }
  }

  private static class NoRetryHandler implements HttpMethodRetryHandler {

    static final NoRetryHandler INSTANCE = new NoRetryHandler();

    public boolean retryMethod(HttpMethod httpmethod, IOException ioexception, int i) {
      return false;
    }

  }

}