/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.object.lockmanager.api.ThreadID;

import java.util.Set;

public interface KeysForOrphanedValuesResponseMessage extends ClusterMetaDataResponseMessage {

  public void initialize(ThreadID threadID, Set response);

  public Set getKeys();

}