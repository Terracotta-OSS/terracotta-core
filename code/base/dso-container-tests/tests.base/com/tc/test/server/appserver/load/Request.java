/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.load;

import org.apache.commons.httpclient.HttpClient;

import java.net.URL;

public interface Request {

  public void setEnterQueueTime();

  public void setExitQueueTime();

  public void setProcessCompletionTime();

  public URL getUrl();

  public long getEnterQueueTime();

  public long getExitQueueTime();

  public long getProcessCompletionTime();

  public HttpClient getClient();

  public int getAppserverID();

  public String printData();
}
