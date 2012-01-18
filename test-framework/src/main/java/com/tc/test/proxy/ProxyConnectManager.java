/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.proxy;

public interface ProxyConnectManager {
  void setupProxy();

  void proxyDown();

  void proxyUp();

  void startProxyTest();

  void stopProxyTest();

  void setManualControl(boolean manualControl);

  boolean isManualControl();

  void setProxyPort(int port);

  int getProxyPort();

  void setDsoPort(int port);

  int getDsoPort();

  void setProxyDownTime(int milliseconds);

  int getProxyDownTime();

  void setProxyWaitTime(int milliseconds);

  int getProxyWaitTime();

  void close();

  void closeClientConnections();
}
