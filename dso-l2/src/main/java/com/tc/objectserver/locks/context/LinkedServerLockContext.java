/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.locks.context;

import com.tc.net.ClientID;
import com.tc.object.locks.ServerLockContext;
import com.tc.object.locks.ThreadID;

public class LinkedServerLockContext extends ServerLockContext {
  private ServerLockContext next;
  
  public LinkedServerLockContext(ClientID clientID, ThreadID threadID) {
    super(clientID, threadID);
  }

  public ServerLockContext getNext() {
    return next;
  }

  public ServerLockContext setNext(ServerLockContext next) {
    ServerLockContext old = this.next;
    this.next = next;
    return old;
  }
}
