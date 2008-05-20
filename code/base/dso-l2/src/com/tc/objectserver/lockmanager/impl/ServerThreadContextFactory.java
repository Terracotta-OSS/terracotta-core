/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.lockmanager.impl;

import com.tc.net.groups.NodeID;
import com.tc.object.lockmanager.api.ServerThreadID;
import com.tc.object.lockmanager.api.ThreadID;

class ServerThreadContextFactory {
  public static final ServerThreadContextFactory DEFAULT_FACTORY       = new ServerThreadContextFactory();
  public static final String                     ACTIVITY_MONITOR_NAME = "ServerThreadContextFactoryActivityMonitor";

  ServerThreadContext getOrCreate(NodeID nid, ThreadID threadID) {
    return new ServerThreadContext(new ServerThreadID(nid, threadID));
  }
}
