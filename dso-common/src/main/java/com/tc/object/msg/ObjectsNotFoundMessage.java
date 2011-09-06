/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.protocol.tcm.TCMessage;

import java.util.Set;

public interface ObjectsNotFoundMessage extends TCMessage {

  public void initialize(Set missingObjectIDs, long batchId);

  public long getBatchID();

  public Set getMissingObjectIDs();

}
