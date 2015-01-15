/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.ObjectID;

import java.util.Set;

public interface ObjectsNotFoundMessage extends TCMessage {

  public void initialize(Set<ObjectID> missingObjectIDs, long batchId);

  public long getBatchID();

  public Set<ObjectID> getMissingObjectIDs();

}
