/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.locks.context;

import com.tc.net.ClientID;
import com.tc.object.locks.ServerLockContext;
import com.tc.object.locks.ThreadID;
import com.tc.util.Assert;

/**
 * This class is present to ensure that we dont waste memory on saving "next" pointer. This will be useful only when a
 * single context is present in the context queue of the lock.
 */
public class SingleServerLockContext extends ServerLockContext {

  public SingleServerLockContext(ClientID clientID, ThreadID threadID) {
    super(clientID, threadID);
  }

  public ServerLockContext getNext() {
    return null;
  }

  public ServerLockContext setNext(ServerLockContext next) {
    Assert.assertNull(next);
    return null;
  }
}
