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
  public static final ServerThreadContextFactory                     DEFAULT_FACTORY          = new ServerThreadContextFactory();
  public static final String                                         ACTIVITY_MONITOR_NAME    = "ServerThreadContextFactoryActivityMonitor";

  private static final Map<ServerThreadContext, ServerThreadContext> vmServerThreadContextMap = new WeakHashMap<ServerThreadContext, ServerThreadContext>();
  private static final Map<ServerThreadContext, ServerThreadContext> serverThreadContextMap   = new WeakHashMap<ServerThreadContext, ServerThreadContext>();

  private ServerThreadContext subGetOrCreate(NodeID nid, ThreadID threadID,
                                             Map<ServerThreadContext, ServerThreadContext> map) {
    ServerThreadContext stc = new ServerThreadContext(new ServerThreadID(nid, threadID));
    ServerThreadContext sstc = map.get(stc);
    if (sstc == null) {
      map.put(stc, stc);
      sstc = stc;
    }
    return sstc;
  }

  ServerThreadContext getOrCreate(NodeID nid, ThreadID threadID) {
    synchronized (serverThreadContextMap) {
      return subGetOrCreate(nid, threadID, serverThreadContextMap);
    }
  }

  ServerThreadContext vmGetOrCreate(NodeID nid, ThreadID threadID) {
    synchronized (vmServerThreadContextMap) {
      return subGetOrCreate(nid, threadID, vmServerThreadContextMap);
    }
  }
}
