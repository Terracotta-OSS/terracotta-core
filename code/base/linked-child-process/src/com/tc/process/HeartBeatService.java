/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.process;

public class HeartBeatService {
  private static HeartBeatServer server;

  public static synchronized void startHeartBeatService() {
    if (server == null) {
      server = new HeartBeatServer();
      server.start();
    }
  }
  
  public static synchronized void stopHeartBeatServer() {
    if (server != null) {
      server.shutdown();
      server = null;
    }
  }
  
  public static synchronized int listenPort() {
    ensureServerHasStarted();
    return server.listeningPort();
  }
  
  public static synchronized void registerForHeartBeat(int listenPort, String clientName) {
    registerForHeartBeat(listenPort, clientName, false);
  }
  
  public static synchronized void registerForHeartBeat(int listenPort, String clientName, boolean isAppServer) {
    ensureServerHasStarted();
    HeartBeatClient client = new HeartBeatClient(listenPort, clientName, isAppServer);
    client.setDaemon(true);
    client.start();
  }
  
  public static synchronized void sendKillSignalToChildren() {
    ensureServerHasStarted();
    server.sendKillSignalToChildren();
  }
  
  public static synchronized boolean anyAppServerAlive() {
    ensureServerHasStarted();
    return server.anyAppServerAlive();
  }
  
  private static void ensureServerHasStarted() {
    if (server == null) new IllegalStateException("Heartbeat service has not started yet!");
  }
}
