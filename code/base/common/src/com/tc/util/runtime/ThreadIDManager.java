/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.object.locks.ThreadID;

public interface ThreadIDManager {
  public ThreadID getThreadID();
}
