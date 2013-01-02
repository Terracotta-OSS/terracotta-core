/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.object.locks.ThreadID;

public class NullThreadIDMapImpl implements ThreadIDMap {

  @Override
  public void addTCThreadID(final ThreadID threadID) {
    //
  }

  @Override
  public ThreadID getTCThreadID(final Long javaThreadID) {
    return null;
  }

}
