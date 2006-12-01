/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.lockmanager.impl;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.lockmanager.api.ServerThreadID;
import com.tc.object.lockmanager.api.ThreadID;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class ServerThreadContextFactory {
  public static final ServerThreadContextFactory DEFAULT_FACTORY       = new ServerThreadContextFactory();
  public static final String                     ACTIVITY_MONITOR_NAME = "ServerThreadContextFactoryActivityMonitor";

  private final Map                              currentContexts       = Collections.synchronizedMap(new HashMap());

  int getCount() {
    return currentContexts.size();
  }

  ServerThreadContext getOrCreate(ChannelID channelID, ThreadID threadID) {
    ServerThreadID id = new ServerThreadID(channelID, threadID);

    synchronized (currentContexts) {
      ServerThreadContext threadContext = (ServerThreadContext) this.currentContexts.get(id);
      if (threadContext == null) {
        threadContext = new ServerThreadContext(id);
        this.currentContexts.put(id, threadContext);
      }
      return threadContext;
    }
  }

  Object remove(ServerThreadContext context) {
    return currentContexts.remove(context.getId());
  }

  void clear(ChannelID channelID) {
    synchronized (currentContexts) {
      for (Iterator i = currentContexts.values().iterator(); i.hasNext();) {
        ServerThreadContext threadContext = (ServerThreadContext) i.next();
        if (threadContext.getId().getChannelID().equals(channelID)) {
          i.remove();
        }
      }
    }
  }

  void clear() {
    currentContexts.clear();
  }

  void removeIfClear(ServerThreadContext threadCtx) {
    if (threadCtx.isClear()) {
      remove(threadCtx);
    }
  }

  Collection getView() {
    synchronized (currentContexts) {
      return Collections.unmodifiableCollection(currentContexts.values());
    }
  }
}
