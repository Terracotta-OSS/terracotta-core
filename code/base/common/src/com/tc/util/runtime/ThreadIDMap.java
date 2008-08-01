/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.object.lockmanager.api.ThreadID;

/**
 * VM ThreadID to TC Thread ID mapping
 */
public interface ThreadIDMap {

  public void addTCThreadID(ThreadID tcThreadID);

  public Long getTCThreadID(long vmThreadID);

}
