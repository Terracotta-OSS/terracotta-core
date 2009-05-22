/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.object.lockmanager.api.ThreadID;

public class NullThreadIDMapImpl implements ThreadIDMap {

  public void addTCThreadID(ThreadID threadID) {
    //
  }

  public ThreadID getTCThreadID(Thread t) {
    return null;
  }

}
