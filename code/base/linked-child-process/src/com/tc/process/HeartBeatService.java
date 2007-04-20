/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.process;

public class HeartBeatService {
  
  public static synchronized void registerForHeartBeat(int listenPort, Class klass) {
    registerForHeartBeat(listenPort, klass, false);
  }
  
  public static synchronized void registerForHeartBeat(int listenPort, Class klass, boolean isAppServer) {
    
  }
  
  public static synchronized void forceShutdown() {
    
  }
  
  
}
