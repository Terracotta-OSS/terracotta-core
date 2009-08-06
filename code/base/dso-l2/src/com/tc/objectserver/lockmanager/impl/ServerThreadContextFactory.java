/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.lockmanager.impl;

import com.tc.net.NodeID;
import com.tc.object.lockmanager.api.ServerThreadID;
import com.tc.object.lockmanager.api.ThreadID;

import java.util.Map;
import java.util.WeakHashMap;

class ServerThreadContextFactory {
  public static final ServerThreadContextFactory                     DEFAULT_FACTORY        = new ServerThreadContextFactory();
  public static final String                                         ACTIVITY_MONITOR_NAME  = "ServerThreadContextFactoryActivityMonitor";

  private static final Map<ServerThreadContext, ServerThreadContext> serverThreadContextMap = new WeakHashMap<ServerThreadContext, ServerThreadContext>();

  ServerThreadContext getOrCreate(NodeID nid, ThreadID threadID) {
    synchronized (serverThreadContextMap) {
      ServerThreadContext stc = new ServerThreadContext(new ServerThreadID(nid, threadID));
      ServerThreadContext sstc = serverThreadContextMap.get(stc);
      if (sstc == null) {
        serverThreadContextMap.put(stc, stc);
        sstc = stc;
      }
      return sstc;
    }
  }

}
