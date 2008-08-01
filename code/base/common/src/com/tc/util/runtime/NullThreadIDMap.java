/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.object.lockmanager.api.ThreadID;

public class NullThreadIDMap implements ThreadIDMap {

  public void addTCThreadID(ThreadID threadID) {
    //
  }

  public Long getTCThreadID(long l) {
    return null;
  }

}
