/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.lockmanager.impl;

import com.tc.net.NodeID;
import com.tc.object.lockmanager.api.ServerThreadID;
import com.tc.object.lockmanager.api.ThreadID;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

class ServerThreadContextFactory {
  public static final ServerThreadContextFactory                     DEFAULT_FACTORY        = new ServerThreadContextFactory();
  public static final String                                         ACTIVITY_MONITOR_NAME  = "ServerThreadContextFactoryActivityMonitor";

  private static final TCWeakHashMap<ServerThreadContext, ServerThreadContext> serverThreadContextMap = new TCWeakHashMap<ServerThreadContext, ServerThreadContext>();

  ServerThreadContext getOrCreate(NodeID nid, ThreadID threadID) {
    ServerThreadContext stc = new ServerThreadContext(new ServerThreadID(nid, threadID));
    synchronized (serverThreadContextMap) {
      ServerThreadContext sstc = serverThreadContextMap.get(stc);
      if (sstc == null) {
        serverThreadContextMap.put(stc, stc);
        sstc = stc;
      }
      return sstc;
    }
  }

  private static class TCWeakHashMap<K, V>  {

    private final WeakHashMap<K, WeakReference<V>> weakMap = new WeakHashMap<K, WeakReference<V>>();

    public V put(K key, V value) {
      WeakReference<V> ref = weakMap.put(key, new WeakReference<V>(value));
      return (ref == null) ? null : ref.get();
    }

    public V get(Object key) {
      WeakReference<V> ref = weakMap.get(key);
      return (ref == null) ? null : ref.get();
    }
    
    // for testing purpose
    private int size() {
      return weakMap.size();
    }
  }
  
//for testing purpose
  int size() {
    return serverThreadContextMap.size();
  }

}
