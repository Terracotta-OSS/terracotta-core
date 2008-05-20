/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.lockmanager.impl;

import com.tc.net.groups.NodeID;
import com.tc.object.lockmanager.api.ServerThreadID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.util.Assert;

class ServerThreadContext {
  static final ServerThreadContext NULL_CONTEXT = new ServerThreadContext(ServerThreadID.NULL_ID);

  private final boolean            isNull;
  private final ServerThreadID     id;
  private final int                hashcode;

  ServerThreadContext(NodeID nid, ThreadID threadID) {
    this(new ServerThreadID(nid, threadID));
  }

  ServerThreadContext(ServerThreadID stid) {
    Assert.assertNotNull(stid);
    this.id = stid;
    this.isNull = ServerThreadID.NULL_ID.equals(stid);
    this.hashcode = this.id.hashCode();
  }

  public String toString() {
    return "ServerThreadContext@" + System.identityHashCode(this) + "[" + id + "]";
  }

  public int hashCode() {
    return this.hashcode;
  }

  public boolean equals(Object obj) {
    if (obj instanceof ServerThreadContext) {
      ServerThreadContext other = (ServerThreadContext) obj;
      return this.id.equals(other.id);
    }
    return false;
  }

  public ServerThreadID getId() {
    return this.id;
  }

  public boolean isNull() {
    return isNull;
  }

}
