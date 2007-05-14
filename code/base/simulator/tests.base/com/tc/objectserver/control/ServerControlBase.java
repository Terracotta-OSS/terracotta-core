/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.control;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class ServerControlBase implements ServerControl {

  private final List   testSockets = new ArrayList();
  private final int    adminPort;
  private final String host;
  private final int    dsoPort;

  public ServerControlBase(String host, int dsoPort, int adminPort) {
    this.host = host;
    this.dsoPort = dsoPort;
    this.adminPort = adminPort;
  }

  public boolean isRunning() {
    try {
      Socket socket = new Socket(host, adminPort);
      testSockets.add(socket);
      if (!socket.isConnected()) throw new AssertionError();
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  public void clean() {
    for (Iterator i = testSockets.iterator(); i.hasNext();) {
      Socket s = (Socket) i.next();
      try {
        System.out.println("Checking socket: " + s);
        if (s.isConnected()) s.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public int getAdminPort() {
    return adminPort;
  }

  public int getDsoPort() {
    return dsoPort;
  }

  protected String getHost() {
    return host;
  }

}
